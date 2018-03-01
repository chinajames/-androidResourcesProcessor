package com.lint.plus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.Element;

public class CopyResourceScanner {

	private File mResourcesFile = null;
	private File mtargetFile = null;
	private File mSearchFile = null;

	private final Set<Resource> foundResources = new HashSet<Resource>();

    //\\s+ 表示一个或多个空格  .*匹配除 "/n" 之外的任何单个字符 (?=exp) (?!exp)否定字符串 (java|android)或运算 .*?非贪婪模式尽可能少的匹配
	//private String javaImportRegex = "import\\s+(.*?)\\s*;";//import java.io.File;
	private String javaImportRegex = "import\\s+((?!(java|android)).*?)\\s*;";//import com.test.File; 
	private String javaRegex = "R\\.(\\w+)\\.(\\w+)";//R.string.name
	private String xmlRegex = "@(\\w+)/(\\w+)";//@string/name
	private List<String> fileBundle = new ArrayList<String>();
	private List<String> stringBundle = new ArrayList<String>();
	private List<String> arrayBundle = new ArrayList<String>();
	private List<String> attrBundle = new ArrayList<String>();
	private List<String> dimenBundle = new ArrayList<String>();
	private List<String> styleBundle = new ArrayList<String>();
	private List<String> colorBundle = new ArrayList<String>();
	private List<String> otherBundle = new ArrayList<String>();
	private final Set<Resource> otherTypeResources = new HashSet<Resource>();
	private final HashSet<String> mImportFiles = new HashSet<String>();
	private xmlUtils xmlUtils;

	private boolean debug = false;

	public CopyResourceScanner() {
		super();

	}

	protected CopyResourceScanner(String Resources, String Search) {
		super();
		mSearchFile = new File(Search);
		mResourcesFile = new File(Resources);
		//String userDir = System.getProperty("user.dir");
		mtargetFile = new File(mResourcesFile.getParent(), mResourcesFile.getName() + "_lintPlus");
		xmlUtils = new xmlUtils();
	}

	public void setTargetFile(String target) {
		mtargetFile = new File(target);
	}

	public void run() {
	    log(String.format("resourcesFile = %s", mResourcesFile.getAbsolutePath()));
	    log(String.format("searchFile = %s", mSearchFile.getAbsolutePath()));
	    log(String.format("targetFile = %s", mtargetFile.getAbsolutePath()));
		if (!mResourcesFile.exists() || !mSearchFile.exists()) {
			System.err.println("ResourcesFile or SearchFile not exists");
			return;
		}
		/*if (!mtargetFile.exists()) {
			mtargetFile.mkdirs();
		}*/
		log("search in: " + mSearchFile.getAbsolutePath());
		long currentTime = System.currentTimeMillis();
		if (mSearchFile.isFile()) {
			searchFile(mSearchFile, javaRegex);
			searchFile(mSearchFile, xmlRegex);
		} else {
			searchFiles(mSearchFile);
		}
		for (final Resource resource : foundResources) {
			if (debug) {
				log(resource.toString());
			}
			if (resource.getType().equals("layout") || resource.getType().equals("anim") || resource.getType().equals("drawable") || resource.getType().equals("xml")
					|| resource.getType().equals("menu") || resource.getType().equals("mipmap")) {
				fileBundle.add(resource.getName());
			} else if (resource.getType().equals("string")) {
				stringBundle.add(resource.getName());
			} else if (resource.getType().equals("array")) {
				arrayBundle.add(resource.getName());
			} else if (resource.getType().equals("dimen")) {
				dimenBundle.add(resource.getName());
			} else if (resource.getType().equals("style") || resource.getType().equals("attr")) {
				styleBundle.add(resource.getName());
			} else if (resource.getType().equals("color")) {
				colorBundle.add(resource.getName());
			} else if (resource.getType().equals("styleable")) {
				String attrName = getAttrName(resource.getName());
				if (attrName != null && !attrName.equals(""))
					attrBundle.add(attrName);
			} else if (!resource.getType().equals("id")) {
				//剩下为匹配类型 例如bool integer 类型等
				otherTypeResources.add(resource);
				otherBundle.add(resource.getName());
			}
		}
		//添加所有类型到otherBundle，适配所有类型，但是会加大耗时时间
		otherBundle.addAll(stringBundle);
		otherBundle.addAll(arrayBundle);
		otherBundle.addAll(dimenBundle);
		otherBundle.addAll(styleBundle);
		otherBundle.addAll(colorBundle);
		otherBundle.addAll(attrBundle);
		if (debug) {
			log("未识别类型");
			for (final Resource resource : otherTypeResources) {
				log(resource.toString());
			}
		}
		log("search Resources finish ");
		copyResourcesFile(mResourcesFile);
		searchXml(mResourcesFile);
		long time = (System.currentTimeMillis() - currentTime) / 1000;
		log(String.format("run finish use time : %s s  ", time));
		copyImportFiles();
	}

	private void searchXml(File file) {
		//mResourcesFile
		if (file == null || !file.exists())
			return;
		if (file.isDirectory()) {
			for (final File child : file.listFiles()) {
				searchXml(child);
			}
		} else if (file.getName().endsWith("xml")) {
			String parentName = file.getParent();
			String substring = parentName.substring(parentName.lastIndexOf(File.separator), parentName.length());
			if (substring.contains("values")) {
				if (file.getName().contains("arrays.xml")) {
					analysisResourcesXml(file, arrayBundle);
				} else if (file.getName().contains("strings.xml")) {
					analysisResourcesXml(file, stringBundle);
				} else if (file.getName().contains("attrs.xml")) {
					analysisResourcesXml(file, attrBundle);
				} else if (file.getName().contains("dimens.xml")) {
					analysisResourcesXml(file, dimenBundle);
				} else if (file.getName().contains("styles.xml")) {
					analysisResourcesXml(file, styleBundle);
				} else if (file.getName().contains("themes.xml")) {
					analysisResourcesXml(file, styleBundle);
				} else if (file.getName().contains("colors.xml")) {
					analysisResourcesXml(file, colorBundle);
				} else {
					analysisResourcesXml(file, otherBundle);
				}
			}
		}

	}

	private void analysisResourcesXml(File mFile, List<String> list) {
		if (debug)
			log("analysisResourcesXml " + mFile.getAbsolutePath());
		Document srcDocument = xmlUtils.read(mFile);
		if (srcDocument == null) {
			System.err.println(String.format("analysisResourcesXml when SAXReader read in %s", mFile.getName()));
			return;
		}
		Element srcRootElement = srcDocument.getRootElement();
		File targetFile = getTargetFile(mFile);
		Document targetDocument = xmlUtils.createDocument(targetFile, srcDocument);
		List<Element> mtargetElements = targetDocument.getRootElement().elements();
		List<String> sortList = new ArrayList<String>();
		for (int i = 0; i < mtargetElements.size(); i++) {
			sortList.add(mtargetElements.get(i).attributeValue("name"));
		}

		for (Iterator i = srcRootElement.elementIterator(); i.hasNext();) {
			Element element = (Element) i.next();
			String name = element.attributeValue("name");
			if (debug)
				log(String.format("type = %s name = %s", element.getName(), name));
			if (list.contains(name)) {
				int index = sortList.indexOf(name);
				if (index != -1) {
					if (debug)
						log(String.format("Replaces name = %s", name));
					//sortList.set(index, name);
					mtargetElements.set(index, element.createCopy());
				} else {
					sortList.add(name);
					mtargetElements.add(element.createCopy());
				}
			}
		}
		if (mtargetElements.size() >= 1) {
			xmlUtils.writeDocument(targetFile, targetDocument);
		}

	}

	private String getAttrName(String name) {
		String attrName = null;
		String[] styleableAttr = name.split("_");
		if (styleableAttr.length > 1) {
			attrName = styleableAttr[0];
		}
		return attrName;
	}

	private void searchFiles(File file) {
		if (file == null || !file.exists())
			return;
		if (file.isDirectory()) {
			for (final File child : file.listFiles()) {
				searchFiles(child);
			}
		} else if (file.getName().endsWith("java")) {
			searchFile(file, javaRegex);
		} else if (file.getName().endsWith("xml")) {
			searchFile(file, xmlRegex);
		}
	}

	private void searchFile(final File file, final String regex) {
	    log(String.format("searchFile %s", file.getAbsoluteFile()));
		String fileContents = null;
		try {
			fileContents = FileUtilities.getFileContents(file);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		final Matcher matcher = Pattern.compile(regex).matcher(fileContents);

		while (matcher.find()) {
			String type = matcher.group(1);
			String name = matcher.group(2);
			if (debug) {
				log(String.format("type =%s name = %s ", type, name));
			}
			foundResources.add(new Resource(type, name));
		}
		searchImportFile(fileContents);

	}
	public void copyResourcesFile(File file) {
		if (file == null || !file.exists())
			return;
		if (file.isDirectory()) {
			for (final File child : file.listFiles()) {
				copyResourcesFile(child);
			}
		} else {
			String fileName = file.getName();
			int pos = fileName.indexOf('.');
			if (pos == -1) {
				return;
			}
			String subName = fileName.substring(0, pos);
			if (fileBundle.contains(subName)) {
				File dest = getTargetFile(file);
				boolean success = FileUtilities.copyFile(file, dest);
				if (debug) {
					log(String.format("cpoy %s %s", success, dest.getAbsolutePath()));
				}
			}
		}
	}

	private File getTargetFile(File file) {
		String absoluteName = file.getAbsolutePath();
		int baseLength = mResourcesFile.getAbsolutePath().length();
		int lastLength = absoluteName.length();
		File targetFile = new File(mtargetFile, absoluteName.substring(baseLength, lastLength));
		return targetFile;
	}

	public void log(String message) {
		System.out.println(message);
	}
	
	private void searchImportFile(String fileContents) {
        final Matcher matcher = Pattern.compile(javaImportRegex).matcher(fileContents);
        while (matcher.find()) {
            String importPath = matcher.group(1);
            mImportFiles.add(importPath);
        }
        fileContents = null;
    }

    private void copyImportFiles() {
        for (String importPath : mImportFiles) {
            importPath = importPath.replaceAll("\\.", "/");
            File importFile = new File(mResourcesFile, "src/" + importPath + ".java");
            //log(String.format("exists = %s path = %s", importFile.exists(),importFile.getAbsolutePath()));
            File targetImportFile = new File(mtargetFile, "src/" + importPath + ".java");
            if (importFile.exists()) {
                if (!targetImportFile.exists()) {
                    boolean success = FileUtilities.copyFile(importFile, targetImportFile);
                    log(String.format("copyFile =%s ,path = %s ", success, targetImportFile.getAbsolutePath()));
                }
            } else {
                System.err.println(String.format("%s no exists!", importFile.getAbsolutePath()));
            }
        }
    }

}
