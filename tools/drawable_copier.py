#!/usr/bin/python

import sys
import os
import shutil
import argparse

verbose = False

# Do it.
def main(): 
    opts = {"src":"", "dst":"","files":[],"filematch":False,"verbose":False}
    do_argparse(opts)

    src = opts["src"]
    dst = opts["dst"]
    filenames = opts["files"]
    filematch = opts["filematch"]

    global verbose
    verbose = opts["verbose"]

    if not validate_dirs(src, dst):
        sys.exit(-1)

    if src == dst:
        print ("Don't make your destination directory the same as " 
            "your source directory!")
        sys.exit(-1)

    src_descriptors = build_descriptor_array(src, None, filenames, filematch)
    if count_files(src_descriptors) == 0:
        print "No resources found in '" + src + "' to copy. Goodbye!"
        sys.exit()

    print "The following resources will be copied"
    print "...from: " + src
    print "...into: " + dst
    print_descriptor_array(src_descriptors)
    proceed_input = raw_input(PROCEED_MSG)

    if proceed_input != "y" and proceed_input != "yes":
        print CANCEL_MSG
        sys.exit()

    dst_descriptors = build_descriptor_array(dst, None, filenames, filematch)

    overwrite_descriptors = find_overwrites(src_descriptors, dst_descriptors)
    if len(overwrite_descriptors) > 0:
        handle_overwrites(overwrite_descriptors)

    total_copied = copy_files(src, src_descriptors, dst, dst_descriptors, True)

    print "Success! " + str(total_copied) + " files copied."


# Filetypes acknowledged by this script.
FILE_TYPES = [".png", ".svg"]

PROCEED_MSG = "\n\"y\" or \"yes\" to proceed. Anything else to cancel: "
OVERWRITE_MSG = ("\n\"y\" or \"yes\" to overwrite these files."
    " Anything else to cancel: ")
CANCEL_MSG = "Nothing will be copied. Goodbye!"

# Simplified UNIX file descriptor for a single depth directory.
class DirDescriptor(object):
    def __init__(self, name="", files=[]):
        self.name = name
        self.files = files

# Sets up command line argparsing.
def do_argparse(opts):
    parser = argparse.ArgumentParser()
    parser.add_argument("src", 
        help="The directory containing the source drawable-* subdirectories.",
        nargs="?",
        default=os.getcwd())
    parser.add_argument("dst", 
        help="The directory containing the destination drawable-* subdirectories.")
    parser.add_argument("-f", "--files",
        nargs="+",
        help="Will copy only the listed files (if they exist).")
    parser.add_argument("--filematch",
        action="store_true",
        default=False,
        help=("Will find any file that has a provided filename (with -f)"
            " as a substring"))
    parser.add_argument("--verbose",
        action="store_true",
        default=False,
        help="Prints extra details as the script runs.")
    args = parser.parse_args()

    opts["src"] = args.src
    opts["dst"] = args.dst
    opts["files"] = args.files
    opts["filematch"] = args.filematch
    opts["verbose"] = args.verbose

# Validates that the src and dst directories exist.
def validate_dirs(src, dst):
    if not os.path.exists(src):
        print src + " does not exist!"
        return False
    if not os.path.exists(dst):
        print dst + " does not exist!"
        return False    
    return True

# Checks if the file is a valid filetype.
def is_valid_type(filename):
    for ext in FILE_TYPES:
        if filename.endswith(ext):
            return True
    return False

# Checks if the directory is a valid android drawable directory (mostly).
def is_drawable_dir(directory):
    return directory is not None and directory.startswith("drawable")

# Checks that the string is in the collection
def in_set(s, set, partial_match=False):
    for candidate in set:
        if candidate == s:
            return True
        elif partial_match and candidate in s:
            return True
    return False

# Find the matching (based on directory name) descriptor of the src for the dst.
def match_descriptor(src_desc, dst_dir_descriptors):
    for dst_dir_desc in dst_dir_descriptors:
        if src_desc.name == dst_dir_desc.name:
            return dst_dir_desc
    return None

# dir: The root directory
# allowed_subdirs: The list of subdirectories to include. If None, all "drawable*
#               directories are included.
# filenames: The list of filenames to include. If None, all filenames of valid 
#            filetype included.
def build_descriptor_array(dir_root, allowed_subdirs, 
    allowed_filenames, filematch=False):
    descriptors = []

    restrict_subdirs = allowed_subdirs is not None
    restrict_filenames = allowed_filenames is not None

    for item in os.listdir(dir_root):
        dir_path = os.path.join(dir_root, item)
        if os.path.isdir(dir_path):
            if not is_drawable_dir(item):
                continue
            if restrict_subdirs and not in_set(item, allowed_subdirs):
                continue

            files_to_copy = []

            for s_item in os.listdir(dir_path):
                file_path = os.path.join(dir_path, s_item)
                if os.path.isfile(file_path):
                    if is_valid_type(s_item):
                        if restrict_filenames and not in_set(s_item, 
                            allowed_filenames, filematch):
                            continue;
                        files_to_copy.append(s_item)
            descriptors.append(DirDescriptor(item, files_to_copy))
                
    return descriptors

# Prints the descriptors array in pleasing fashion.
def print_descriptor_array(descriptors, print_empty_dirs=False):
    print ""

    if descriptors is None:
        print "Descriptors array is empty."
        return

    for decsriptor in descriptors:
        if not print_empty_dirs and len(decsriptor.files) == 0:
            continue
        print "\t+ " + decsriptor.name
        for filename in decsriptor.files:
            print "\t\t- " + filename

# Totals the sum of all files in the DirDescriptors.
def count_files(descriptors):
    total = 0
    for d in descriptors:
        total += len(d.files)
    return total


# Gets the set of files in src_files that also exist in dst_files.
def find_duplicates(src_files, dst_files):
    duplicates = []

    for src_file in src_files:
        if in_set(src_file, dst_files):
            duplicates.append(src_file)

    return duplicates

# Finds the directories and files that will be overwritten in the copy.
def find_overwrites(src_descriptors, dst_descriptors):
    overwrite_descriptors = []

    for src_descriptor in src_descriptors:
        dst_descriptor = match_descriptor(src_descriptor, dst_descriptors)
        if dst_descriptor is None:
            continue
        dups = find_duplicates(src_descriptor.files, dst_descriptor.files)
        if len(dups) > 0:
            overwrite_descriptor = DirDescriptor(src_descriptor.name, dups) 
            overwrite_descriptors.append(overwrite_descriptor)
    
    return overwrite_descriptors

# Handles user interaction for overwriting duplicate files.    
def handle_overwrites(overwrite_descriptors):
    print "\n!!! Copy conflict(s) found!!!"
    print_descriptor_array(overwrite_descriptors)

    cnfrm_input = raw_input(OVERWRITE_MSG).lower()
    
    if cnfrm_input == "y" or cnfrm_input == "yes":
        print "Overwriting..."
    else:
        print CANCEL_MSG
        sys.exit()

# Perform the copy from a src subdir to the matched dst subdir
def copy_files_by_subdir(src_root, src_descriptor, dst_root, dst_descriptor):
    
    if src_descriptor.name != dst_descriptor.name:
        print src_descriptor.name + " : '" + dst_descriptor.name
        raise Exception("ERROR: Copying between non-matched directories.")

    src_prefix = os.path.join(src_root, src_descriptor.name)
    dst_prefix = os.path.join(dst_root, dst_descriptor.name)

    copied_count = 0
    for src_file in src_descriptor.files:
        src_filename = os.path.join(src_prefix, src_file)    
        dst_filename = os.path.join(dst_prefix, src_file)

        shutil.copyfile(src_filename, dst_filename)
        copied_count += 1

        if (verbose):
            print "Copying " + os.path.join(src_descriptor.name, src_file) + "..."


    return copied_count
        
# Creates the directory and returns its descriptor.
# WARNING: Race condition between os.path.exists() and os.makedirs().
#          Not deemed a concern in this script, though. (famous last words)
def do_mkdirs(dir_root, dir_name):
    dir_path = os.path.join(dir_root, dir_name)
    if os.path.exists(dir_path):
        raise Exception(dir_path + " already exists!")

    if (verbose):
        print "mkdirs: " + dir_path

    os.makedirs(dir_path)

    return DirDescriptor(dir_name, [])

# Actually copies the files from src to dst, overwriting any files in dst
# with the same name as files in src.
def copy_files(src_root, src_descriptors, dst_root, dst_descriptors, mkdirs):
    total_copied = 0

    for src_descriptor in src_descriptors:
        dst_descriptor = match_descriptor(src_descriptor, dst_descriptors);

        # Dst dir doesn't exist. Only create if there are files to copy.
        if dst_descriptor is None and mkdirs and len(src_descriptor.files) > 0:
            dst_descriptor = do_mkdirs(dst_root, src_descriptor.name)

        if dst_descriptor is not None:
            total_copied += copy_files_by_subdir(src_root, src_descriptor, 
                dst_root, dst_descriptor)

    return total_copied


if __name__ == "__main__":
    main()