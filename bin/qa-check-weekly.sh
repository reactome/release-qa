#!/bin/bash
#
# Runs the weekly QA checks.
#
# * File structure:
#   /usr/local/share/reactome/
#     bin/
#       qa-check-weekly.sh
#     lib/
#       CuratorQA-jar-with-dependencies.jar
#       SlicingTool-jar-with-dependencies.jar
#       SliceQA-jar-with-dependencies.jar
#       Notify-jar-with-dependencies.jar
#     QAReports/
#       # the collected QA check reports, e.g.:
#       2018-09-12/
#         CuratorQA/
#           ... # gk_central QA reports
#         SliceQA/
#           ... # slice QA reports
#     CuratorQA/
#       resources/
#         auth.properties
#         log4j.properties
#     SlicingTool/
#       slicingTool.prop
#       SliceLog4j.properties
#       Species.txt
#       topics.txt
#     SliceQA/
#       resources/
#         auth.properties
#         log4j2.properties
#         ... # other Slice QA config files
#     Notify/
#       resources/
#         curators.csv
#         log4j2.properties
#

# The standard QA check location.
# This script is assumed to reside in the bin subdirectory
# of the check location.
here=$(dirname `realpath $0`)
share=`dirname $here`

# The date file and database name suffix.
date=`date "+%Y%m%d"`

# The QA report location.
reports_dir=$share/QAReports

## gk_central QA ##

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

# Run the Curator QA checks against gk_central.
echo "Running the gk_central QA checks..."
(cd $curator_qa_dir; java -Xmx8G -jar $curator_qa_jar)

# Copy the output to the reports area.
curator_qa_rpt_dir="$reports_dir/$date/CuratorQA"
[ -e "$curator_qa_rpt_dir" ] || mkdir -p $curator_qa_rpt_dir
[ -n "$(ls -A $curator_qa_rpt_dir)" ] && rm -f $curator_qa_rpt_dir/*
[ -n "$(ls -A $curator_qa_out_dir)" ] && cp -f $curator_qa_out_dir/* $curator_qa_rpt_dir
echo "The gk_central reports are in $curator_qa_rpt_dir."


## Slicing ##

# The Slicing Tool location.
slicing_dir="$share/SlicingTool"
# The Slicing Tool jar file.
slicing_jar="$share/lib/SlicingTool-jar-with-dependencies.jar"
# The Slicing Tool configuration file.
slicing_prop_file="$slicing_dir/slicingTool.prop"
# The slice database name.
slice_db="test_slice_$date"
# The previous slice date.
prev_date=`sed -nE 's/releaseDate[ ]*=[ ]*(.*)/\1/p' $slicing_prop_file | \
           tr -d '-'`
# The previous slice database.
prev_db="test_slice_$prev_date"

# Make the slice.
slicing_opts="--slicingDbName $slice_db"
echo "Running the slicing tool..."
(cd $slicing_dir; java -Xmx8G -jar $slicing_jar $slicing_opts)
rc=$?
if [ "${rc}" -ne 0 ]; then
    (>&2 echo "Slicing was not successful")
    exit ${rc}
fi
echo "Slicing database created: $slice_db"

# Drop the previous slice database.
# If there is a reports directory, then the database
# was created by this script.
if [ "$prev_db" != "$slice_db" ] && [ -e $reports_dir/$prev_date ]; then
    prev_db="test_slice_$prev_date"
    db_user=`sed -nE '/dbUser[ ]*=[ ]*(.*)/\1/p' $slicing_prop_file`
    db_pswd=`sed -nE '/dbPwd[ ]*=[ ]*(.*)/\1/p' $slicing_prop_file`
    mysql -u"$db_user" -p"$db_pswd" -e "DROP DATABASE IF EXISTS $prev_db" 2>&1 | \
      grep -v "\[Warning\] Using a password"
fi


## Slice QA ##

# The Slice QA location.
slice_qa_dir="$share/SliceQA"
if [ ! -e "$slice_qa_dir" ]; then
    (>&2 echo "The Slice QA directory was not found: $slice_qa_dir")
    exit 1
fi
# The Slice QA output directory.
slice_qa_out_dir="$slice_qa_dir/output"
[ -e "$slice_qa_out_dir" ] || mkdir -p $slice_qa_out_dir
[ -n "$(ls -A $slice_qa_out_dir)" ] && rm -f $slice_qa_out_dir/*

# Run the Slice QA.
slice_qa_jar="$share/lib/SliceQA-jar-with-dependencies.jar"
slice_qa_opts="--dbName $slice_db"
echo "Running the slice QA checks..."
(cd $slice_qa_dir; java -Xmx8G -jar $slice_qa_jar $slice_qa_opts)
rc=$?
if [ "${rc}" -ne 0 ]; then
    (>&2 echo "Slice QA execution was not successful")
    exit ${rc}
fi

# Copy the output to the reports area.
slice_qa_rpt_dir="$reports_dir/$date/SliceQA"
[ -e "$slice_qa_rpt_dir" ] || mkdir -p $slice_qa_rpt_dir
[ -n "$(ls -A $slice_qa_rpt_dir)" ] && rm -f $slice_qa_rpt_dir/*
[ -n "$(ls -A $slice_qa_out_dir)" ] && cp -f $slice_qa_out_dir/* $slice_qa_rpt_dir
echo "The gk_central reports are in $slice_qa_rpt_dir."

echo "The Slice QA reports are in $slice_qa_out_dir."


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
(cd $notify_dir; java $java_opts -jar $notify_jar $reports_dir/$date)
rc=$?
if [ "${rc}" -ne 0 ]; then
    (>&2 echo "Notification was not successful")
    exit ${rc}
fi
