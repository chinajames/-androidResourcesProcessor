package com.lint.plus;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.Element;

public class LintMissingTranslation {
	public Map<String, HashSet<String>> missTranslateMapData = new HashMap<String, HashSet<String>>();
	//"`developer`" is not translated in "en" (English), "zh-CN" (Chinese: China)
	//private String messageRegex = "\"`(\\w+)`\" is not translated in \"(\\w+)\"";
	private String messageRegex = "\"`(\\w+)`\".*\"(\\w+)\"";
	//private String translatedRegex = "\"(\\w+)\" \\((\\w+)\\)";//"ar-EG" (Arabic: Egypt)
	//private String translatedRegex = "\"(.*?)\" \\((.*?)\\),?";//"ar-EG" (Arabic: Egypt)
	private String translatedRegex = "\"(.*?)\"";//"ar-EG" (Arabic: Egypt)
	private File resourcesFile;
	private File missingTranslationFile;
	private File lintBatFile;
	private File translateFile;
	private List<String> defaultStringListFile = new ArrayList<String>();
	private xmlUtils xmlUtils;
	private boolean sortXml = true;

	public LintMissingTranslation(String resources) {
		resourcesFile = new File(resources);
		translateFile = new File(resources, "translate");
		missingTranslationFile = new File(resourcesFile, "missingTranslation.xml");
		lintBatFile = new File("F:\\android-sdk-windows\\tools\\lint.bat");
		defaultStringListFile.add("res/values/strings.xml");
		defaultStringListFile.add("res/values/arrays.xml");
		defaultStringListFile.add("res/values-zh-rCN/strings.xml");
		defaultStringListFile.add("res/values-zh-rCN/arrays.xml");
		defaultStringListFile.add("res/values-en/strings.xml");
		defaultStringListFile.add("res/values-en/arrays.xml");
		xmlUtils = new xmlUtils();
	}

	public void run() {
		if (!resourcesFile.exists()) {
			System.err.println(String.format("%s no exists!", resourcesFile.getAbsolutePath()));
			return;
		}
		if (missingTranslationFile.exists()) {
			missingTranslationFile.delete();
		}
		runLintbat();
		if (!missingTranslationFile.exists()) {
			System.err.println(String.format("%s no exists!", missingTranslationFile.getAbsolutePath()));
			return;
		}
		analysisResultXml();
		copyMissingResources();
	}

	private void copyMissingResources() {
		for (String key : missTranslateMapData.keySet()) {
			HashSet<String> hashSet = missTranslateMapData.get(key);
			/*for (String value : hashSet) {
				log(String.format("key = %s value = %s", key, value));
			}*/
			for (int i = 0; i < defaultStringListFile.size(); i++) {
				String filePath = defaultStringListFile.get(i);
				File defaultStirngxmlFile = new File(resourcesFile, filePath);
				if (defaultStirngxmlFile.exists()) {
					copyResources(defaultStirngxmlFile, filePath.replace("res", key), hashSet);
				}
			}

		}
	}

	private void copyResources(File defaultFile, String subFile, HashSet<String> hashSet) {
		Document srcDocument = xmlUtils.read(defaultFile);
		if (srcDocument == null)
			return;
		Document newDocument = xmlUtils.createDocument(srcDocument.getXMLEncoding());
		Element newRootElement = newDocument.getRootElement();
		Element rootElement = srcDocument.getRootElement();
		for (Iterator iterator = rootElement.elementIterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			String name = element.attributeValue("name");
			if (hashSet.contains(name)) {
				newRootElement.add(element.createCopy());
			}
		}
		File targetFile = new File(translateFile, subFile);
		xmlUtils.writeDocument(targetFile, newDocument);
	}

	public void analysisResultXml() {
		Document document = xmlUtils.read(missingTranslationFile);
		if (document == null)
			return;
		Element issuesElements = document.getRootElement();
		for (Iterator issue = issuesElements.elementIterator(); issue.hasNext();) {
			Element issueElement = (Element) issue.next();
			String message = issueElement.attributeValue("message");
			//log(message);
			final Matcher matchertranLate = Pattern.compile(translatedRegex).matcher(message);
			int num = 0;
			String missString = "";
			String temp = "";
			List<String> languages = new ArrayList<String>();
			while (matchertranLate.find()) {
				temp = matchertranLate.group(1);
				num++;
				if (num == 1) {
					missString = temp.replaceAll("`", "");
				} else {
					languages.add(temp);
					putMissingTranslationData(temp, missString);
				}
			}
			log(String.format("Missing  = %s language = %s", missString, languages.toString()));

		}

	}

	public void putMissingTranslationData(String key, String value) {
		if (missTranslateMapData.get(key) == null) {
			HashSet<String> data = new HashSet<String>();
			data.add(value);
			missTranslateMapData.put(key, data);
		} else {
			HashSet<String> data = missTranslateMapData.get(key);
			data.add(value);
			missTranslateMapData.put(key, data);
		}
	}

	private void runLintbat() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("cmd.exe /c start /b ");
		buffer.append(lintBatFile.getAbsolutePath());
		buffer.append(" ");
		buffer.append("--check ");
		buffer.append("\"MissingTranslation\" ");
		buffer.append("--xml ");
		buffer.append(missingTranslationFile.getAbsolutePath());
		buffer.append(" ");
		buffer.append(resourcesFile.getAbsolutePath());
		log(buffer.toString());

		try {
			Process process = Runtime.getRuntime().exec(buffer.toString());
			InputStreamReader inputStr = new InputStreamReader(process.getInputStream());
			BufferedReader br = new BufferedReader(inputStr);
			String temp = "";
			while ((temp = br.readLine()) != null) {
				log(temp);
				//lint.bat 文件中添加echo finish_Lint_Bat作为结束语句
				if (temp.contains("finish_Lint_Bat")) {
					break;
				}
			}
			process.destroy();
			br.close();
			inputStr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void log(String tips) {
		System.out.println(tips);
	}
}
