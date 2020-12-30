#!/bin/bash
#
# Prunes all but the n most recent slice databases
# and reports, where n is the optional command argument
# (default 2).
#

# The number of weekly reports to keep (default 2).
n=$1
if [ -z "$n" ]; then
    n=2
fi

# This script is assumed to reside in the bin subdirectory
# of the check location. BASH_SOURCE[0] followed by readlink
# is preferred to $0 (cf. https://gist.github.com/olegch/1730673).
here="${BASH_SOURCE[0]}"
while [ -h "$here" ]; do
    here="$(readlink "$here")"
done
bin_dir=`dirname $here`
share=`(cd $bin_dir/..; pwd)`

# The QA report location.
reports_dir=$share/QAReports
if [ ! -e $reports_dir ]; then
    (>&2 echo "Reports directorty not found: $v")
    exit 1
fi

# The Slicing Tool location.
slicing_dir="$share/SlicingTool"
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
    exit 0
fi
# The number of reports and dbs to prune.
prune_cnt=$(( $dates_cnt - $n ))
# The dates to prune.
dates=`(cd $reports_dir; ls -d * | grep -E '[[:digit:]]{8}' | head -n $prune_cnt)`

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
    mysql -u"$db_user" -p"$db_pswd" -e "DROP DATABASE IF EXISTS $prune_db" 2>&1 | \
      grep -v "\[Warning\] Using a password"
    rm -r $reports_dir/$prune_date
done