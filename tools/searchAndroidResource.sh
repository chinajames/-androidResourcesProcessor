#!/bin/bash
#example  
#merge_strings project_src/packages/apps/Settings project_dest/packages/apps/Settings decodeFile 
#命令行参考：http://man.linuxde.net/
#本文件以unix编码 set ff=unix
set -u   #当你使用未初始化的变量时，让bash自动退出 
set -e   # fail on errors 一但有任何一个语句返回非真的值，则退出bash

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
#</resources> 插入到这行之前  
    local string_file="$1"  
    local line="$2"  
    local trim_line=`echo $2 | grep -Po '\S.*\S'`  
    local name=`echo $trim_line | grep -Po "(?<=name=\").*?(?=\")"`  
    local line_no=`grep -n "\b$name\b" "$string_file" | grep -Po "^\d+"`  
    #a.检查是否有这个字段  
    if [ "$line_no" != "" ]; then  
        #echo "line_no=$line_no" "$string_file"  
        local result=`grep -n "$trim_line" "$string_file"`  
        #b.检查是否能完整匹配。如果不能，则删除旧的，添加新的  
        if [ "$result" = "" ]; then  
            sed -i "$line_no""d" "$string_file"  
            sed -i '/<\/resources>/i\'"$line" "$string_file"  
        fi  
    else  
        sed -i '/<\/resources>/i\'"$line" "$string_file"  
    fi  
}
#本程序没有用到，将results里面的字符串结果复制到相对应当目录文件中
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
#本程序没有用到，从结果文件中复制相对应的资源与文件
function readFromResultsFile(){  
while read line; do  
	local type=${line% *}
	local name=${line#* }
	#-a 与 -o或 !非
	if [ $type = "drawable" -o $type = "color" ]; 
	then  
		echo "type = ${type} name = ${name} is drawable or color"
	else
		echo "type = ${type} name = ${name}"
	fi
done < results.txt    
}

#从传入的参数开始解析所有android的R.**.name格式的资源
#sed 用法
#-e<script>或--expression=<script>：以选项中的指定的script来处理输入的文本文件；
#-f<script文件>或--file=<script文件>：以选项中指定的script文件来处理输入的文本文件；
#-n或--quiet或——silent：仅显示script处理后的结果；
#regular.sed 所谓的script文件内容是以下两条sed 替换命令 查找替换类似R.id.name 类似@string/name 相关android资源文件
#s/.*R\.(\w{2,})\.(\w+).*/\1 \2/gp
#s/.*@(\w{2,})\/(\w+).*/\1 \2/gp
function readFromAnalysisFile(){  
if [ -d $decodeFile ]; then
#find ./FileExplorer/src/ -type f |xargs sed -n -r -f regular.sed   |sort -u
	#content=`find ${decodeFile} -type f |xargs sed -n -r -f regular.sed |sort -u` #测试ok
	content=`find ${decodeFile} -type f |xargs sed -n -r -e 's/.*R\.(\w{2,})\.(\w+).*/\1 \2/gp' -e 's/.*@(\w{2,})\/(\w+).*/\1 \2/gp' |sort -u` #不引用regular.sed 用-e 多行命令执行
else
	#content=`sed -n -r -f regular.sed ${decodeFile} |sort -u` #测试ok
	content=`sed -n -r -e 's/.*R\.(\w{2,})\.(\w+).*/\1 \2/gp' -e 's/.*@(\w{2,})\/(\w+).*/\1 \2/gp' ${decodeFile} |sort -u` #不引用regular.sed 用-e 多行命令执行
	
fi  
IFS_OLD=$IFS  
IFS=$'\n'   
for line in $content; do  
    local type=${line% *}
	local name=${line#* }
	#-a 与 -o或 !非
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
#根据查找到的资源进行二次搜索，并进行copyResource复制的动作
function searchResource(){
if [ "$regex_with_all_string" == "" ]; then
	echo "regex_with_all_string = ${regex_with_all_string} is null"
	return 0;
fi
result_list=`grep -Pr "$regex_with_all_string" $src_dir/res/values*/*.xml`    #先执行语句，若有错误就报错，再赋值到result_list变量
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

#复制字符串等类型的资源文件
function copyResource(){
local line=$1 #$1 function 传参 第一个参数，不是外部传进来的参数
country_new=`echo "$line" | grep -Po "^.*?\.xml"`  
string_file="$dest_dir/res/$country_new"  
line=`echo "$line" | grep -Po "(?<=:).*"`  
if [ ! -f "$string_file" ]; then  
    make_new_xml_file "$country_new"  
fi  
insert_line "$string_file" "$line"
#echo "insert_line $string_file $line"
}

#复制文件类型的资源文件
function searchResourceFiles(){
# find ./FileExplorer -name ic_background* |xargs -I{} cp {} ./wtest
#find $src_dir -name "$1*" |xargs -I{} cp {} $dest_dir
#find $src_dir -name "$1*" | xargs -I{} echo {} "$dest_dir "
find_result=`find $src_dir -name "$1*"`
IFS_OLD=$IFS  
IFS=$'\n'  
for src_copy_file in $find_result; do  
     #echo "searchResourceFiles ${src_copy_file} $dest_dir/${src_copy_file#*/}"
	 #basename dest_copy_file #获取文件名
	 #cpoy_dirname=`dirname dest_copy_file` #获取目录名
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
	exit 1 #退出
fi  
#主入口，开始分析资源内容
readFromAnalysisFile


