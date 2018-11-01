#!/usr/bin/env bash
#
# Runs the weekly QA checks.
#

# This script is assumed to reside in the bin subdirectory
# of the QA check location. BASH_SOURCE[0] followed by readlink
# is preferred to $0 (cf. https://gist.github.com/olegch/1730673).
here="${BASH_SOURCE[0]}"
resolved="$here"
while [ -h "$resolved" ]; do
    resolved="$(readlink "$resolved")"
done
# The relative path of the script directory.
rel_bin_dir=`dirname $resolved`
# The absolute path of the script directory.
bin_dir=`(cd $rel_bin_dir; pwd)`
# The QA check root directory is the script directory parent.
qa_check_root=`dirname $bin_dir`

# The platform.
kernel="$(uname -s)"
case "$kernel" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    CYGWIN*)    machine=Cygwin;;
    *)          machine="UNKNOWN"
esac

# Displays the help message.
usage() {
    echo "Usage: $0 [-h|--help] [-d|--dry-run] [--] [DATE]"
}

HELP=false     # Display help.
DRY_RUN=false  # Display subcommands rather than running them.
CLEAN=false    # Delete the created report directory if dry run is set.
ECHO=""        # Precede subcommands with echo if and only if dry run is set.

# The standard option parsing idiom.
while true; do
    case "$1" in
        -h | --help )    HELP=true; shift ;;
        -d | --dry-run ) DRY_RUN=true; shift ;;
        -- ) shift; break ;;
        * ) break ;;
    esac
done

# Display help if asked.
if $HELP; then
    usage
    exit 0
fi

# Extraneous arguments are an error.
if (( "$#" > 1 )); then
    usage
    exit 1
fi

# If the dry run option is set, then echo commands
# rather than running them.
if $DRY_RUN; then
    ECHO="echo"
fi

# The optional date is the sole argument.
date_arg="$1"
if [ -n "$date_arg" ]; then
    # MacOS date command differs from Linux.
    if [ "$machine" == "Mac" ]; then
        date_opts="-f %Y%m%d -j $date_arg"
    else
        date_opts="-d $date_arg"
    fi
else
    date_opts=""
fi
# The date file and database name suffix.
date=`date $date_opts "+%Y%m%d"`
# The date suitable for printing.
date_hyphenated=`date $date_opts "+%Y-%m-%d"`

# The QA report location.
reports_dir=$qa_check_root/QAReports
current_rpt_dir="$reports_dir/$date"
# If the date was specified on the command line,
# then create the reports directory if necessary.
# This will cause slicing to be skipped below.
if [ ! -e "$current_rpt_dir" ]; then
    if [ -n "$date_arg" ]; then
        mkdir "$current_rpt_dir"
        echo "Created the reports directory: $current_rpt_dir."
    fi
    # If dry run, then the target location is either
    # created here or later. In either case, delete
    # the directory afterwords.
    if $DRY_RUN; then
        CLEAN=true
    fi
fi

echo "Starting the $date_hyphenated weekly QA checks..."

## Slicing ##

# The Slicing Tool location.
slicing_dir="$qa_check_root/SlicingTool"
# The Slicing Tool jar file.
slicing_jar="$qa_check_root/lib/SlicingTool-jar-with-dependencies.jar"
# The slice database name.
slice_db="test_slice_$date"

# It is assumed that there is an existing slice database
# if and only if the reports subdirectory exists for the date.
if [ -e "$current_rpt_dir" ]; then
    echo "Skipping slicing, since the reports directory already exists: $current_rpt_dir."
    echo "The slicing database is therefore assumed to exist: $slice_db."
else
    slicing_opts="--slicingDbName $slice_db"
    echo "Running the slicing tool..."
    (cd $slicing_dir; $ECHO java -Xmx8G -jar $slicing_jar $slicing_opts)
    rc=$?
    if [ "${rc}" -ne 0 ]; then
        (>&2 echo "Slicing was not successful")
        exit ${rc}
    fi
    echo "Slicing database created: $slice_db."
fi

# The db command line option overrides the config file in
# the QA checks below.
db_opt="--dbName $slice_db"


## Curator QA ##

# The CuratorQA location.
curator_qa_dir="$qa_check_root/CuratorQA"
if [ ! -e "$curator_qa_dir" ]; then
    (>&2 echo "The CuratorQA directory was not found: $curator_qa_dir")
    exit 1
fi
# The Curator QA jar file.
curator_qa_jar="$qa_check_root/lib/CuratorQA-jar-with-dependencies.jar"
# The Curator QA output area.
curator_qa_out_dir="$curator_qa_dir/QA_Output"
[ -e "$curator_qa_out_dir" ] || mkdir $curator_qa_out_dir
if ! $DRY_RUN; then
    [ -n "$(ls -A $curator_qa_out_dir)" ] && rm -f $curator_qa_out_dir/*
fi

# Run the Curator QA checks against the slice database.
echo "Running the Curator QA checks..."
(cd $curator_qa_dir; $ECHO java -Xmx8G -jar $curator_qa_jar $db_opt)

# Copy the output to the reports area.
curator_qa_rpt_dir="$current_rpt_dir/CuratorQA"
[ -e "$curator_qa_rpt_dir" ] || mkdir -p $curator_qa_rpt_dir
if ! $DRY_RUN; then
    [ -n "$(ls -A $curator_qa_rpt_dir)" ] && rm -f $curator_qa_rpt_dir/*
    [ -n "$(ls -A $curator_qa_out_dir)" ] && cp -f $curator_qa_out_dir/* $curator_qa_rpt_dir
fi
echo "The Curator QA reports are in $curator_qa_rpt_dir."


## Release QA ##

# The Release QA location.
rls_qa_dir="$qa_check_root/ReleaseQA"
if [ ! -e "$rls_qa_dir" ]; then
    (>&2 echo "The Release QA directory was not found: $rls_qa_dir")
    exit 1
fi
# The Release QA output directory.
rls_qa_out_dir="$rls_qa_dir/output"
[ -e "$rls_qa_out_dir" ] || mkdir -p $rls_qa_out_dir
if ! $DRY_RUN; then
    [ -n "$(ls -A $rls_qa_out_dir)" ] && rm -f $rls_qa_out_dir/*
fi

# Run the Release QA.
rls_qa_jar="$qa_check_root/lib/ReleaseQA-jar-with-dependencies.jar"
echo "Running the Release QA checks..."
(cd $rls_qa_dir; $ECHO java -Xmx8G -jar $rls_qa_jar $db_opt)
rc=$?
if [ "${rc}" -ne 0 ]; then
    (>&2 echo "Release QA execution was not successful")
    exit ${rc}
fi

# Copy the output to the reports area.
rls_qa_rpt_dir="$current_rpt_dir/ReleaseQA"
[ -e "$rls_qa_rpt_dir" ] || mkdir -p $rls_qa_rpt_dir
if ! $DRY_RUN; then
    [ -n "$(ls -A $rls_qa_rpt_dir)" ] && rm -f $rls_qa_rpt_dir/*
    [ -n "$(ls -A $rls_qa_out_dir)" ] && cp -f $rls_qa_out_dir/* $rls_qa_rpt_dir
fi

echo "The Release QA reports are in $rls_qa_out_dir."


## Difference ##

# Find the diffs.
dates=`(cd $reports_dir; ls -d * | grep -E '[[:digit:]]{8}' | sort -r | head -n 2)`
if (( `echo $dates | wc -w` == 2 )); then
    echo "Taking the difference between the `echo $dates | sed 's/ / and /'` reports..."
    (cd $reports_dir; $ECHO $bin_dir/diff.sh $dates)
    rc=$?
    if [ "${rc}" -ne 0 ]; then
        (>&2 echo "Diff was not successful")
        # This is not fatal; a warning suffices.
    fi
fi

## Notification ##

# The Notify location.
notify_dir="$qa_check_root/Notify"
if [ ! -e "$notify_dir" ]; then
    (>&2 echo "The Notify directory was not found: $notify_dir")
    exit 1
fi

# Run the notifier.
notify_jar="$qa_check_root/lib/Notify-jar-with-dependencies.jar"
# For some reason, the log config is not picked up in the
# notifier. Work around this by specifying it in an option.
notify_log_cfg="$notify_dir/resources/log4j2.properties"
java_opts="-Dlog4j.configurationFile=$notify_log_cfg"
echo "Running the notifier..."
(cd $notify_dir; $ECHO java $java_opts -jar $notify_jar $current_rpt_dir)
rc=$?
if [ "${rc}" -ne 0 ]; then
    (>&2 echo "Notification was not successful")
    exit ${rc}
fi

# Prune all but the current and previous databases and reports.
echo "Pruning obsolete databases and reports..."
prune="$bin_dir/prune.sh"
if [ ! -e $prune ]; then
    (>&2 echo "Prune script was not found: $prune")
    exit 1
fi
if $DRY_RUN; then
    prune_args='--dry-run'
else
    prune_args=""
fi
eval "$prune $prune_args"
rc=$?
if [ "${rc}" -ne 0 ]; then
    (>&2 echo "Pruning was not successful")
    exit ${rc}
fi

# Delete the created dry run report directory, if necessary.
if $CLEAN; then
    rm -r $current_rpt_dir
fi

echo "The $date_hyphenated weekly QA check is completed."
echo "Reports are in $current_rpt_dir."
exit 0
