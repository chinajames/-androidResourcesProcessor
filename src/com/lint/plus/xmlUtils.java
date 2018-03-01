package com.lint.plus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class xmlUtils {

	public Document sortXml(File referenceXmlFile, File targetXmlFile, boolean removeComment) {
		SAXReader reader = new SAXReader();
		Document referenceDocument = null;
		Document targetDocument = null;
		try {
			referenceDocument = reader.read(referenceXmlFile);
			targetDocument = reader.read(targetXmlFile);
		} catch (DocumentException e) {
			e.printStackTrace();
			return null;
		}
		return sortXml(referenceDocument, targetDocument, removeComment);
	}

	public Document sortXml(Document referenceDocument, Document targetDocument) {
		return sortXml(referenceDocument, targetDocument, false);
	}

	public Document sortXml(Document referenceDocument, Document targetDocument, boolean removeComment) {
		if (removeComment) {
			removeDocumentComment(targetDocument);
		}
		List<Element> referenceElements = referenceDocument.getRootElement().elements();
		List<Element> targetElements = targetDocument.getRootElement().elements();
		List<String> sortList = new ArrayList<String>();
		for (int i = 0; i < targetElements.size(); i++) {
			sortList.add(targetElements.get(i).attributeValue("name"));
		}
		int targetIndex = 0;
		for (int i = 0; i < referenceElements.size() && targetIndex < targetElements.size(); i++) {
			String referenceName = referenceElements.get(i).attributeValue("name");
			String targetName = targetElements.get(targetIndex).attributeValue("name");
			if (referenceName == null) {
				continue;
			}
			if (referenceName.equals(targetName)) {
				targetIndex++;
				continue;
			}
			int replaceIndex = sortList.indexOf(referenceName);
			if (replaceIndex == -1) {
				//log(String.format("%s is null", referenceName));
				continue;
			}
			Element targetElement = targetElements.get(replaceIndex);
			Element tempElement = targetElements.get(targetIndex);
			sortList.set(targetIndex, referenceName);
			targetElements.set(targetIndex, targetElement.createCopy());
			sortList.set(replaceIndex, tempElement.attributeValue("name"));
			targetElements.set(replaceIndex, tempElement.createCopy());
			targetIndex++;
		}
		return targetDocument;
	}

	public Document createDocument(String encoding) {
		Document newDocument = DocumentHelper.createDocument();
		newDocument.setXMLEncoding(encoding);
		Element newRootElement = newDocument.addElement("resources");
		//newRootElement.add(new Namespace("android", "http://schemas.android.com/apk/res/android"));//添加命名空间
		newRootElement.addNamespace("xliff", "urn:oasis:names:tc:xliff:document:1.2");//添加命名空间
		return newDocument;
	}

	public Document createDocument(File targetFile, Document srcDocument) {
		Document document = read(targetFile);
		if (document == null) {
			//System.err.println(String.format("error:createDocument when SAXReader read in %s", targetFile.getName()));
			if (targetFile.exists())
				targetFile.delete();
			document = DocumentHelper.createDocument();
		}else{
			return document;//资源文件已经存在
		}
		document.setXMLEncoding(srcDocument.getXMLEncoding());
		Element rootElement = null;
		if (document.getRootElement() == null) {
			rootElement = document.addElement(srcDocument.getRootElement().getName());
		} else {
			rootElement = document.getRootElement();
		}
		rootElement.add(new Namespace("xliff", "urn:oasis:names:tc:xliff:document:1.2"));//添加命名空间
		return document;
	}

	public Document read(File file) {
		if(!file.exists())
			return null;
		SAXReader reader = new SAXReader();
		Document document = null;
		try {
			document = reader.read(file);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}

	public void writeDocument(File targetFile, Document document) {
		//log("writeDocument " + targetFile.getAbsolutePath());
		if (!targetFile.getParentFile().exists()) {
			targetFile.getParentFile().mkdirs();
		}
		OutputFormat format = OutputFormat.createPrettyPrint();//输出格式  
		format.setEncoding(document.getXMLEncoding());//设置编码  
		format.setIndentSize(4);//缩进空格数
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(targetFile), document.getXMLEncoding());
			XMLWriter writer = new XMLWriter(outputStreamWriter, format);
			//writer.setEscapeText(false);//设置特殊符号不被转义,此处二次操作有bug
			writer.write(document);//写入新文件  
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void log(String msg) {
		System.out.println(msg);
	}

	//xml文档对象去掉注释后输出字符串 
	public void removeDocumentComment(Document doc) {
		Iterator<Node> nodeIt = doc.nodeIterator();
		//删除根节点上的注释，以及Xml文件中与根节点同级的注释内容  
		while (nodeIt.hasNext()) {
			Node node = nodeIt.next();
			if (node.getNodeType() == Node.COMMENT_NODE) {
				//System.out.println(node.toString());
				doc.remove(node);
			}
		}
		Element root = doc.getRootElement();
		deleteXmlNotation(root);
	}

	private void deleteXmlNotation(Element ele) {
		Iterator<Node> nodes = ele.nodeIterator();
		List<Node> rmNodes = new ArrayList<Node>();
		//循环收集可以删除的节点  
		while (nodes.hasNext()) {
			Node subNode = nodes.next();
			if (subNode.getNodeType() == Node.COMMENT_NODE) {
				rmNodes.add(subNode);
				rmNodes.add(nodes.next());
				//System.out.println(subNode.toString());
			}
		}
		//删除收集到的节点  
		for (Node node : rmNodes) {
			ele.remove(node);
		}
		//递归，处理下级节点  
		Iterator<Element> eleIt = ele.elementIterator();
		while (eleIt.hasNext()) {
			deleteXmlNotation(eleIt.next());
		}
	}

	/**
	 * 此Comparator弃用，比较的过程中会造成List<Element>发生变化
	 */
	public class sortComparator implements Comparator<Element> {
		int time = 0;
		LinkedHashMap<String, Integer> sortLinkedHashMap;

		public sortComparator(LinkedHashMap<String, Integer> sortLinkedHashMap) {
			this.sortLinkedHashMap = sortLinkedHashMap;
		}

		@Override
		public int compare(Element o1, Element o2) {
			//o1 后者  o2前者 o1>o2则为升序
			time++;
			String comparator1 = o1.attributeValue("name");
			String comparator2 = o2.attributeValue("name");
			System.out.println(String.format("time = %d comparator1 = %s %d comparator2 = %s %d", time, comparator1, sortLinkedHashMap.get(comparator1), comparator1,
					sortLinkedHashMap.get(comparator2)));
			if (comparator1 == null || comparator2 == null) {
				return 1;
			}
			return sortLinkedHashMap.get(comparator1) - sortLinkedHashMap.get(comparator2);
		}

	}

}
