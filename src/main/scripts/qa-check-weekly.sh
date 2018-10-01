#!/bin/bash
#
# Runs the weekly QA checks.
#
# This script is assumed to reside in the bin subdirectory
# of the check location. BASH_SOURCE[0] followed by readlink
# is preferred to $0 (cf. https://gist.github.com/olegch/1730673).
here="${BASH_SOURCE[0]}"
while [ -h "$here" ]; do
    here="$(readlink "$here")"
done
bin_dir=`dirname $here`
share=`(cd $bin_dir/..; pwd)`

# The date file and database name suffix.
date=`date "+%Y%m%d"`
# The date suitable for printing.
date_hyphenated=`date "+%Y-%m-%d"`

# The QA report location.
reports_dir=$share/QAReports
current_rpt_dir="$reports_dir/$date"

echo "Starting the $date_hyphenated weekly QA checks..."

## Slicing ##

# The Slicing Tool location.
slicing_dir="$share/SlicingTool"
# The Slicing Tool jar file.
slicing_jar="$share/lib/SlicingTool-jar-with-dependencies.jar"
# The slice database name.
slice_db="test_slice_$date"

# If reports exist for the current date, then there
# is a database. Otherwise, make the slice database.
if [ -e "$current_rpt_dir" ]; then
    echo "Skipping slicing, since the reports directory already exists: $current_rpt_dir"
else
    slicing_opts="--slicingDbName $slice_db"
    echo "Running the slicing tool..."
    (cd $slicing_dir; java -Xmx8G -jar $slicing_jar $slicing_opts)
    rc=$?
    if [ "${rc}" -ne 0 ]; then
        (>&2 echo "Slicing was not successful")
        exit ${rc}
    fi
    echo "Slicing database created: $slice_db"
fi

# The db command line option overrides the config file in
# the QA checks below.
db_opt="--dbName $slice_db"


## Curator QA ##

# The CuratorQA location.
curator_qa_dir="$share/CuratorQA"
if [ ! -e "$curator_qa_dir" ]; then
    (>&2 echo "The CuratorQA directory was not found: $curator_qa_dir")
    exit 1
fi
# The Curator QA jar file.
curator_qa_jar="$share/lib/CuratorQA-jar-with-dependencies.jar"
# The Curator QA output area.
curator_qa_out_dir="$curator_qa_dir/QA_Output"
[ -e "$curator_qa_out_dir" ] || mkdir $curator_qa_out_dir
[ -n "$(ls -A $curator_qa_out_dir)" ] && rm -f $curator_qa_out_dir/*

# Run the Curator QA checks against the slice database.
echo "Running the Curator QA checks..."
(cd $curator_qa_dir; java -Xmx8G -jar $curator_qa_jar $db_opt)

# Copy the output to the reports area.
curator_qa_rpt_dir="$current_rpt_dir/CuratorQA"
[ -e "$curator_qa_rpt_dir" ] || mkdir -p $curator_qa_rpt_dir
[ -n "$(ls -A $curator_qa_rpt_dir)" ] && rm -f $curator_qa_rpt_dir/*
[ -n "$(ls -A $curator_qa_out_dir)" ] && cp -f $curator_qa_out_dir/* $curator_qa_rpt_dir
echo "The Curator QA reports are in $curator_qa_rpt_dir."


## Release QA ##

# The Release QA location.
rls_qa_dir="$share/ReleaseQA"
if [ ! -e "$rls_qa_dir" ]; then
    (>&2 echo "The Release QA directory was not found: $rls_qa_dir")
    exit 1
fi
# The Release QA output directory.
rls_qa_out_dir="$rls_qa_dir/output"
[ -e "$rls_qa_out_dir" ] || mkdir -p $rls_qa_out_dir
[ -n "$(ls -A $rls_qa_out_dir)" ] && rm -f $rls_qa_out_dir/*

# Run the Release QA.
rls_qa_jar="$share/lib/ReleaseQA-jar-with-dependencies.jar"
echo "Running the Release QA checks..."
(cd $rls_qa_dir; java -Xmx8G -jar $rls_qa_jar $db_opt)
rc=$?
if [ "${rc}" -ne 0 ]; then
    (>&2 echo "Release QA execution was not successful")
    exit ${rc}
fi

# Copy the output to the reports area.
rls_qa_rpt_dir="$current_rpt_dir/ReleaseQA"
[ -e "$rls_qa_rpt_dir" ] || mkdir -p $rls_qa_rpt_dir
[ -n "$(ls -A $rls_qa_rpt_dir)" ] && rm -f $rls_qa_rpt_dir/*
[ -n "$(ls -A $rls_qa_out_dir)" ] && cp -f $rls_qa_out_dir/* $rls_qa_rpt_dir

echo "The Release QA reports are in $rls_qa_out_dir."


## Notification ##

# The Notify location.
notify_dir="$share/Notify"
if [ ! -e "$notify_dir" ]; then
    (>&2 echo "The Notify directory was not found: $notify_dir")
    exit 1
fi

# Run the notifier.
notify_jar="$share/lib/Notify-jar-with-dependencies.jar"
# For some reason, the log config is not picked up in the
# notifier. Work around this by specifying it in an option.
java_opts="-Dlog4j.configurationFile=resources/log4j2.properties"
echo "Running the notifier..."
(cd $notify_dir; java $java_opts -jar $notify_jar $current_rpt_dir)
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
eval "$prune"
rc=$?
if [ "${rc}" -ne 0 ]; then
    (>&2 echo "Pruning was not successful")
    exit ${rc}
fi

echo "The $date_hyphenated weekly QA check is completed."
exit 0
