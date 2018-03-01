#!/bin/bash

function usage {
	name=$0
	echo "Usage: $name"
	echo ""
	echo "Searches for and copies all versions of an Android resource"
	echo "(currently only drawables) from one project directory into "
	echo "another, preserving version qualifiers."
	echo ""
	echo "Usage: $name inFile outDir"
	echo ""
	echo "inFile   The resource to copy in all versions."
	echo "outDir   The res folder to copy the files to."
	exit 1
}

IN_FILE=$1
OUT_DIR=$2

if [[ -z $IN_FILE || -z $OUT_DIR ]];then
	usage
fi

# Get the file metadata
in_file_name=$(basename "$IN_FILE")
in_res_folder=$(echo "$IN_FILE" | xargs dirname | xargs basename)
in_res_type=$(echo "$in_res_folder" | cut -d '-' -f1)
echo "Copying $in_res_type files named $in_file_name"

# Get the base res directory
in_res_dir=$IN_FILE
until [[ "$in_res_dir" == */res ]]; do
	in_res_dir=`dirname $in_res_dir`
done

# Get the output res directory
out_res_dir=$OUT_DIR
until [[ "$out_res_dir" == */res ]]; do
	out_res_dir=`dirname $out_res_dir`
done

# File all versions of this file
search_dir="$in_res_dir/$in_res_type-*"
files=$(find $search_dir -name "$in_file_name" -print)
for file in $files; do

	# Get the file's resource dir
	file_dir=$(echo "$file" | xargs dirname | xargs basename)

	# Create the output directory if it doesn't exist
	out_dir="$out_res_dir/$file_dir"
	if [ ! -d "$out_dir" ];then
		echo "Creating missing directory: $out_dir"
		mkdir -p "$out_dir"
	fi

	# Copy the file
	out_file="$out_res_dir/$file_dir/$in_file_name"
	cp "$file" "$out_file"
	echo "$file -> $out_file"
done