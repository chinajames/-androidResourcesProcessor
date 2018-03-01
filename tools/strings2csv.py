#!/usr/bin/python
# -*- coding: utf-8 -*-
#title       :strings2csv.py
#author      :yejinbing
#date        :20131203
#description :find all strings.xml, then write the string or string-array
#             attrs to a csv file

from sets import Set
from xml.dom import minidom
import csv
import getopt
import glob
import os
import re
import sys

__version__ = '0.7'

class StringAttr():
    '''string.xml class, has attrs:string.xml file path, language and
    string or string-array dict.'''

    def __init__(self, path='', language=''):
        self.path = path
        self.language = language
        self.strings = {}

    def __str__(self):
        return self.path + '  ' + self.language

def parseStrings(stringPath):
    r"""parse strings.xml to a dict.

    Args:
        stringPath: string.xml file path

    Returns:
        A dict, like:{'name': str/list, ...}.
        if object is string, the value is a str,
        else if object is string-array, the value is a list. For example:

        {
            'install'   : 'install',
            'uninstall' : 'uninsatll',
            'arrays'    : ['array1', 'array2']
        }"""
    xmldoc = minidom.parse(stringPath)
    strElements = xmldoc.getElementsByTagName('string')
    strArrayElements = xmldoc.getElementsByTagName('string-array')
    def mapString(x):
        name = x.attributes['name'].value
        if x.firstChild:
            if x.firstChild.nodeType in (minidom.Node.TEXT_NODE,
                    minidom.Node.CDATA_SECTION_NODE):
                value = x.firstChild.data.encode('utf-8')
            else:
                value = x.firstChild.toxml('utf-8')
            return (name, value)
        else:
            return (name, '')
    def mapStringArray(x):
        if x.firstChild:
            if x.firstChild.nodeType in (minidom.Node.TEXT_NODE,
                    minidom.Node.CDATA_SECTION_NODE):
                return x.firstChild.data.encode('utf-8')
            else:
                return x.firstChild.toxml('utf-8')
        else:
            return ''

    strDict = dict(map(mapString, strElements))
    strArrayDict = dict(\
            map(lambda x: (x.attributes['name'].value, (\
                map(mapStringArray, x.getElementsByTagName('item')))), \
            strArrayElements))
    return dict(strDict, **strArrayDict)

def parseLanguage(valuePath):
    r"""parse language string by value directory name.

    Args:
        valuePath: The 'value' directory path

    Returns:
        A language str, For example:

        values        -- >
        values-zh-rCN -- > zh-rCN
        values-en     -- > en"""
    reLanguage = re.compile(r'values-(\S+)$')
    m = reLanguage.search(valuePath)
    return m and m.group(1) or ''

def getAllNames(stringAttrs):
    r"""string or string-array name from all strings.xml file.

    Args:
        stringAttrs: All StringAttr objects

    Returns:
        A list about string or string-array name from
        all strings.xml file union, For example:

        ['install', 'uninstall', 'arrays']"""
    names = Set()
    for stringAttr in stringAttrs:
        for string in stringAttr.strings.keys():
            names.add(string)
    return list(names)

def generateArrayRows(name, stringAttrs):
    r"""generate csv rows for string-array.

    Args:
        name: The string-array name
        stringAttrs: The all StringAttr objects

    Returns:
        A list about csv row, first and last row's name is
        format as 'string-array(name)', the other rows's name
        is empty, For example:

        [
            ['string-array(arrays)', 'item1', '项目1'],
            [''                    , 'item2', '项目2'],
            [''                    , 'item3', '项目3'],
            ['string-array(arrays)', 'item4', '项目4'],
        ]"""
    rows = []
    row = [name,]
    rows.append(['string-array(%s)' % name,])
    i = 0
    while True:
        row = ['',]
        end = True
        for stringAttr in stringAttrs:
            value = stringAttr.strings.get(name, None)
            if value and type(value) == type(list()) and i < len(value):
                row.append(value[i])
                end = False
            else:
                row.append('')
        if end:
            break
        i += 1
        rows.append(row)
    rows.append(['string-array(%s)' % name,])
    return rows


def generateRows(name, stringAttrs):
    r"""generate csv rows for string or string-array.

    Args:
        name: A string or string-array name
        stringAttrs: The all StringAttr objects

    Returns:
        A list about csv row, if is string, only have one row,
        if is string-array, will call the function 'generateArrayRows',
        return many rows. For Example:

        string:
            [['install', 'install', '安装'],]

        string-array:
            [
                ['string-array(arrays)', 'item1', '项目1'],
                [''                    , 'item2', '项目2'],
                [''                    , 'item3', '项目3'],
                ['string-array(arrays)', 'item4', '项目4'],
            ]"""
    row = [name,]
    for stringAttr in stringAttrs:
        value = stringAttr.strings.get(name, None)
        if value:
            if type(value) == type(str()):
                row.append(value)
            elif type(value) == type(list()):
                return generateArrayRows(name, stringAttrs)
            else:
                row.append('')
        else:
            row.append('')

    return [row,]

def saveAsCsv(names, stringAttrs, csvFile):
    r"""save result to csv file.

    Args:
        names: string or string-array names
        stringAttrs: The all StringAttr objects
        csvFile: The output file(.csv) file instance

    Returns:
        None"""
    writer = csv.writer(csvFile)
    import codecs
    csvFile.write(codecs.BOM_UTF8)
    stringAttrs.sort(cmp=lambda x,y:cmp(x.language,y.language))
    #if language is empty, use 'default'
    langs = map(lambda x: x.language or 'default', stringAttrs)
    langs.insert(0, '')
    writer.writerow(langs)
    for name in names:
        for row in generateRows(name, stringAttrs):
            writer.writerow(row)

def getStringAttrs(resPath):
    r"""get all StringAttr objects in the res path.

    Args:
        resPath: The res directory path

    Returns:
        A StringAttr list"""
    values = glob.glob(os.path.join(resPath, 'values*'))
    stringAttrs = []
    for value in values:
        stringXml = glob.glob(os.path.join(value, 'strings.xml'))
        if len(stringXml) > 0:
            lang = parseLanguage(value)
            strAttr = StringAttr(stringXml[0], lang)
            stringAttrs.append(strAttr)
    return stringAttrs

def handleNames(names, stringAttrs, needFilter=False):
    r"""handle names list: sort and filter.

    Args:
        names: All string or string-array names
        stringAttrs: All StringAttr objects
        needFilter: is filter only has empty in a language

    Returns:
        A str list"""
    def cmpName(x, y):
        emptyX = 0
        emptyY = 0
        for stringAttr in stringAttrs:
            if not x in stringAttr.strings:
                emptyX += 1
            if not y in stringAttr.strings:
                emptyY + 1
        if emptyX > emptyY:
            return -1
        elif emptyX < emptyY:
            return 1
        else:
            return cmp(x, y)

    def filterName(x):
        for stringAttr in stringAttrs:
            if not x in stringAttr.strings:
                return True
        return False

    if needFilter:
        names = filter(filterName, names)
    names.sort(cmp=cmpName)
    return names

def getResPath(apkPath):
    r"""get the res directory path in the apk source root path.

    Args:
        apkPath: The apk source root path or res path

    Returns:
        The path about the res directory, is don't find,
        return None"""
    resPath = glob.glob(os.path.join(apkPath, 'res'))
    if len(resPath) > 0:
        return resPath[0]
    else:
        values = glob.glob(os.path.join(apkPath, 'values*'))
        if len(values) > 0:
            return apkPath
        else:
            return None

def usage():
    print '''\
Usage: string2csv.py [option] [outputfile]
This program arrange launguage to *.csv file for strings.xml
option:
  -s, --source: apk source directory
  -o, --output: csv output file
  -f : need filter only has empyt
      -v, --version : Prints the version number
      -h, --help    : Display this help'''

def main(argv):
    try:
        opts, args = getopt.getopt(
                argv,
                "vhs:o:fd",
                ["version", "help", "source" "output"])
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    apkPath = os.getcwd()
    csvFile = 'languages.csv'
    needFilter = False
    debug = False

    for opt, arg in opts:
        if opt in ('-h', '--help'):
            usage()
            sys.exit()
        elif opt in ('-v', '--version'):
            print 'Version %s' % __version__
            sys.exit()
        elif opt in ('-d'):
            debug = True
        elif opt in ('-s', '--source'):
            apkPath = arg
        elif opt in ('-o', '--output'):
            csvFile = arg
        elif opt in ('-f'):
            needFilter = True

    resPath = getResPath(apkPath)
    if not resPath:
        print "No res or values directory in '%s'" % apkPath
        print "You can use commond like:'./string2csv.py -s ApkSourcePath'."
        sys.exit()

    stringAttrs = getStringAttrs(resPath)
    for stringAttr in stringAttrs:
        stringAttr.strings = parseStrings(stringAttr.path)
    names = getAllNames(stringAttrs)
    names = handleNames(names, stringAttrs, needFilter)
    if debug:
        saveAsCsv(names, stringAttrs, sys.stdout)
    else:
        saveAsCsv(names, stringAttrs, open(csvFile, 'w'))

if __name__ == '__main__':
    main(sys.argv[1:])
