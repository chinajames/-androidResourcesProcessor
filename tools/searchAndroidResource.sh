#!/bin/bash
#example  
#merge_strings project_src/packages/apps/Settings project_dest/packages/apps/Settings decodeFile 
#�����вο���http://man.linuxde.net/
#���ļ���unix���� set ff=unix
set -u   #����ʹ��δ��ʼ���ı���ʱ����bash�Զ��˳� 
set -e   # fail on errors һ�����κ�һ����䷵�ط����ֵ�����˳�bash

src_dir="$1"  
dest_dir="$2"  
decodeFile="$3"   
regex_with_all_string=""

function make_new_xml_file() {  
    local country="$1"  
    local folder=${country%/*}  
    if [ ! -d "$dest_dir/res/$folder" ]; then
        mkdir "$dest_dir/res/$folder" -p
    fi  
    local xml_path="$dest_dir/res/$country"  
    touch "$xml_path"  
    echo '<?xml version="1.0" encoding="utf-8"?>' > "$xml_path" #line1  
    echo '<resources>' >> "$xml_path" #line2  
    echo '</resources>' >> "$xml_path" #line3  
    #echo "make_new_xml_file country = ${country} xml_path = ${xml_path}"
}
function insert_line() {  
#</resources> ���뵽����֮ǰ  
    local string_file="$1"  
    local line="$2"  
    local trim_line=`echo $2 | grep -Po '\S.*\S'`  
    local name=`echo $trim_line | grep -Po "(?<=name=\").*?(?=\")"`  
    local line_no=`grep -n "\b$name\b" "$string_file" | grep -Po "^\d+"`  
    #a.����Ƿ�������ֶ�  
    if [ "$line_no" != "" ]; then  
        #echo "line_no=$line_no" "$string_file"  
        local result=`grep -n "$trim_line" "$string_file"`  
        #b.����Ƿ�������ƥ�䡣������ܣ���ɾ���ɵģ�����µ�  
        if [ "$result" = "" ]; then  
            sed -i "$line_no""d" "$string_file"  
            sed -i '/<\/resources>/i\'"$line" "$string_file"  
        fi  
    else  
        sed -i '/<\/resources>/i\'"$line" "$string_file"  
    fi  
}
#������û���õ�����results������ַ���������Ƶ����Ӧ��Ŀ¼�ļ���
function copyData(){
while read line; do  
    country_new=`echo "$line" | grep -Po "^.*?\.xml"`  
    string_file="$dest_dir/res/$country_new"  
    line=`echo "$line" | grep -Po "(?<=:).*"`  
    if [ ! -f "$string_file" ]; then  
        make_new_xml_file "$country_new"  
    fi  
    insert_line "$string_file" "$line"  
done < results.txt    
}
#������û���õ����ӽ���ļ��и������Ӧ����Դ���ļ�
function readFromResultsFile(){  
while read line; do  
	local type=${line% *}
	local name=${line#* }
	#-a �� -o�� !��
	if [ $type = "drawable" -o $type = "color" ]; 
	then  
		echo "type = ${type} name = ${name} is drawable or color"
	else
		echo "type = ${type} name = ${name}"
	fi
done < results.txt    
}

#�Ӵ���Ĳ�����ʼ��������android��R.**.name��ʽ����Դ
#sed �÷�
#-e<script>��--expression=<script>����ѡ���е�ָ����script������������ı��ļ���
#-f<script�ļ�>��--file=<script�ļ�>����ѡ����ָ����script�ļ�������������ı��ļ���
#-n��--quiet�򡪡�silent������ʾscript�����Ľ����
#regular.sed ��ν��script�ļ���������������sed �滻���� �����滻����R.id.name ����@string/name ���android��Դ�ļ�
#s/.*R\.(\w{2,})\.(\w+).*/\1 \2/gp
#s/.*@(\w{2,})\/(\w+).*/\1 \2/gp
function readFromAnalysisFile(){  
if [ -d $decodeFile ]; then
#find ./FileExplorer/src/ -type f |xargs sed -n -r -f regular.sed   |sort -u
	#content=`find ${decodeFile} -type f |xargs sed -n -r -f regular.sed |sort -u` #����ok
	content=`find ${decodeFile} -type f |xargs sed -n -r -e 's/.*R\.(\w{2,})\.(\w+).*/\1 \2/gp' -e 's/.*@(\w{2,})\/(\w+).*/\1 \2/gp' |sort -u` #������regular.sed ��-e ��������ִ��
else
	#content=`sed -n -r -f regular.sed ${decodeFile} |sort -u` #����ok
	content=`sed -n -r -e 's/.*R\.(\w{2,})\.(\w+).*/\1 \2/gp' -e 's/.*@(\w{2,})\/(\w+).*/\1 \2/gp' ${decodeFile} |sort -u` #������regular.sed ��-e ��������ִ��
	
fi  
IFS_OLD=$IFS  
IFS=$'\n'   
for line in $content; do  
    local type=${line% *}
	local name=${line#* }
	#-a �� -o�� !��
	if [ $type = "drawable" -o $type = "layout" -o $type = "anim" -o $type = "menu" ]; then  
		#echo "type = ${type} name = ${name} drawable or layout"
		searchResourceFiles $name
	elif [ $type = "string" -o $type = "dimen" -o $type = "bool" ]; then  
		#echo "type = ${type} name = ${name} string or dimen"
		regex_with_all_string=$regex_with_all_string"name=\"${name}\"|"  
	else
		echo "type = ${type} name = ${name}"
	fi 
done  
IFS=$IFS_OLD 
regex_with_all_string=${regex_with_all_string%|*}  
#echo ${regex_with_all_string}
#ls $src_dir/res/values*/*.xml
searchResource
}
#���ݲ��ҵ�����Դ���ж���������������copyResource���ƵĶ���
function searchResource(){
if [ "$regex_with_all_string" == "" ]; then
	echo "regex_with_all_string = ${regex_with_all_string} is null"
	return 0;
fi
result_list=`grep -Pr "$regex_with_all_string" $src_dir/res/values*/*.xml`    #��ִ����䣬���д���ͱ����ٸ�ֵ��result_list����
echo "$regex_with_all_string"
if [ -f "results.txt" ]; then    
	rm results.txt
fi  
touch results.txt   
IFS_OLD=$IFS  
IFS=$'\n'  
for line in $result_list; do  
     echo "${line#*res/}" >> results.txt
	 copyResource "${line#*res/}" 
done  
IFS=$IFS_OLD 
#echo "searchResource finish"
}

#�����ַ��������͵���Դ�ļ�
function copyResource(){
local line=$1 #$1 function ���� ��һ�������������ⲿ�������Ĳ���
country_new=`echo "$line" | grep -Po "^.*?\.xml"`  
string_file="$dest_dir/res/$country_new"  
line=`echo "$line" | grep -Po "(?<=:).*"`  
if [ ! -f "$string_file" ]; then  
    make_new_xml_file "$country_new"  
fi  
insert_line "$string_file" "$line"
#echo "insert_line $string_file $line"
}

#�����ļ����͵���Դ�ļ�
function searchResourceFiles(){
# find ./FileExplorer -name ic_background* |xargs -I{} cp {} ./wtest
#find $src_dir -name "$1*" |xargs -I{} cp {} $dest_dir
#find $src_dir -name "$1*" | xargs -I{} echo {} "$dest_dir "
find_result=`find $src_dir -name "$1*"`
IFS_OLD=$IFS  
IFS=$'\n'  
for src_copy_file in $find_result; do  
     #echo "searchResourceFiles ${src_copy_file} $dest_dir/${src_copy_file#*/}"
	 #basename dest_copy_file #��ȡ�ļ���
	 #cpoy_dirname=`dirname dest_copy_file` #��ȡĿ¼��
	 #dirname dest_copy_file
	 dest_copy_file="$dest_dir/${src_copy_file#*/}"
	 dest_copy_dir="${dest_copy_file%/*}"
	 #echo "${src_copy_file} ${dest_copy_file} ${dest_copy_dir}"
	if [ ! -d "${dest_copy_dir}" ]; then
        mkdir "${dest_copy_dir}" -p
	fi  
	cp "${src_copy_file}" "${dest_copy_file}"

done  
IFS=$IFS_OLD 
}

if [ ! -e $src_dir ]; then
	echo "$src_dir no exist!"
	exit 1
elif [ ! -e $decodeFile ]; then
	echo "$decodeFile no exist!"
	exit 1 #�˳�
fi  
#����ڣ���ʼ������Դ����
readFromAnalysisFile


