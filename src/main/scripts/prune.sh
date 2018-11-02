#!/usr/bin/env bash
#
# Prunes all but the n most recent slice databases
# and reports, where n is the optional command argument
# (default 4).
#

# See qa-check-weekly.sh for an explanation of the idiom below.
here="${BASH_SOURCE[0]}"
resolved="$here"
while [ -h "$resolved" ]; do
    resolved="$(readlink "$resolved")"
done
rel_bin_dir=`dirname $resolved`
bin_dir=`(cd $rel_bin_dir; pwd)`
qa_check_root=`dirname $bin_dir`

# Displays the help message.
usage() {
    echo "Usage: $0 [-h|--help] [-d|--dry-run] [-n count] [--] [reports_dir]"
}

HELP=false     # Display help.
DRY_RUN=false  # Display subcommands rather than running them.
ECHO=""        # Precede subcommands with echo if and only if dry run is set.
n=4            # The number of slices to prune.

# The standard option parsing idiom.
while true; do
    case "$1" in
        -h | --help )    HELP=true; shift ;;
        -d | --dry-run ) DRY_RUN=true; shift ;;
        -n ) n="$2"; shift; shift ;;
        -- ) shift; break ;;
        * ) break ;;
    esac
done

# Display help if asked.
if $HELP; then
    usage
    exit 0
fi

# If the dry run option is set, then echo commands
# rather than running them.
if $DRY_RUN; then
    ECHO="echo"
fi

# Must have a valid count and at most one other argument.
if [ -z "$n" ] || (("$#" > 1)); then
    usage
    exit 1
fi
if (( "$#" == 1 )); then
    reports_dir=`(cd $1; pwd)`
else
    reports_dir="$qa_check_root/QAReports"
fi
if [ ! -e $reports_dir ]; then
    (>&2 echo "Reports directory not found: $reports_dir")
    exit 1
fi

# The Slicing Tool location.
slicing_dir="$qa_check_root/SlicingTool"
# The Slicing Tool configuration file.
slicing_prop_file="$slicing_dir/slicingTool.prop"
if [ ! -e $slicing_prop_file ]; then
    (>&2 echo "Slicing property file not found: $slicing_prop_file")
    exit 1
fi

# If there is a reports directory, then the database was created
# by this script.
#
# Note: It is simpler to use the Linux head -n -2 command below.
# However, that it is not supported on BSD-based OSes, e.g. Mac OS.
# The following code is compatible with both Linux and Mac OS.
dates_cnt=`(cd $reports_dir; ls -d * | wc -w)`
# If nothing to prune, then we are done.
if (( $dates_cnt <= $n )); then
    echo "No report slice subdirectories found to prune."
    exit 0
fi
# The number of reports and dbs to prune.
prune_cnt=$(( $dates_cnt - $n ))
# The dates to prune.
dates=`ls $reports_dir | grep -E '[[:digit:]]{8}' | head -n $prune_cnt`

# Extract the db user and password from the property file.
# The sed command prints the the dbUser or dbPwd property
# assignment right-hand side, ignoring spaces.
db_user=`sed -nE "s/^dbUser[ ]*=[ ]*(.*)/\1/p" $slicing_prop_file`
db_pswd=`sed -nE "s/^dbPwd[ ]*=[ ]*(.*)/\1/p" $slicing_prop_file`

# For each date in the pruning list,
# delete the corresponding slice database
# and the reports subdirectory.
for prune_date in $dates; do
    prune_db="test_slice_$prune_date"
    echo "Dropping database $prune_db..."
    drop_cmd="DROP DATABASE IF EXISTS $prune_db"
    $ECHO mysql -u"$db_user" -p"$db_pswd" -e "$drop_cmd" 2>&1 | \
      grep -v "\[Warning\] Using a password"
    rpt_subdir="$reports_dir/$prune_date"
    echo "Deleting report directory $rpt_subdir..."
    $ECHO rm -r $rpt_subdir
done
