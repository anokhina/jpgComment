/*******************************************************************************
 * Copyright 2017 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ru.org.sevn.jpgcomment;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.StringWriter;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JpgImage {
	
	public static void test() {
		try {
			copyImage(new File("misc/smile.jpg"), new File("misc/smile1.jpg"), "New comment");
			// Here we get an exception:
			/*
javax.imageio.IIOException: JFIF APP0 must be first marker after SOI
	at com.sun.imageio.plugins.jpeg.JPEGMetadata.<init>(Unknown Source)
	at com.sun.imageio.plugins.jpeg.JPEGImageReader.getImageMetadata(Unknown Source) 
			 */
			copyImage(new File("misc/smile1.jpg"), new File("misc/smile2.jpg"), "Next comment");
			// it can't read images it writes himself
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    static void copyImage( File fileIn, File fileOut, String newComment ) {
        try {

            ImageInputStream stream = ImageIO.createImageInputStream(fileIn);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

            if (readers.hasNext()) {

                // pick the first available ImageReader
                ImageReader reader = readers.next();

                // attach source to the reader
                reader.setInput(stream, true);

                // read metadata of first image
                int idx = reader.getMinIndex(); 
               	IIOMetadata metadata = reader.getImageMetadata(idx);
                
                //reader.setInput(iis);
                //IIOImage image = reader.read(0);
                IIOImage iimage = reader.readAll(idx, reader.getDefaultReadParam());
                RenderedImage img = reader.read(0);                

                String[] names = metadata.getMetadataFormatNames();
                int length = names.length;
                for (int i = 0; i < length; i++) {
                    System.out.println( "Format name: " + names[ i ] );
                    Element tree = (Element)metadata.getAsTree(names[i]);
                    
                    NodeList comNL = tree.getElementsByTagName("com");
                    IIOMetadataNode comNode;
                    if (comNL.getLength() == 0) {
                        comNode = new IIOMetadataNode("com");
                        NodeList markerSequenceNL = tree.getElementsByTagName("markerSequence");
                        if (markerSequenceNL.getLength() > 0) {
	                        Node markerSequenceNode = markerSequenceNL.item(0);
	                        markerSequenceNode.insertBefore(comNode,markerSequenceNode.getFirstChild());
                        }
                    } else {
                        comNode = (IIOMetadataNode) comNL.item(0);
                    }
                    comNode.setUserObject(newComment.getBytes("UTF-8"));
                    
                    metadata.setFromTree(names[i], tree);
                    showMetadata(tree);
                }
                
                ImageOutputStream streamOut = ImageIO.createImageOutputStream(fileOut);
                ImageWriter writer = ImageIO.getImageWriter(reader);
                writer.setOutput(streamOut);
                
//                JPEGImageWriteParam param = new JPEGImageWriteParam(Locale.getDefault());
//                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//                param.setCompressionQuality(1);
//                param.setOptimizeHuffmanTables(true);
                
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
                
                // save the image with new comment inside
                IIOImage iioimage = new IIOImage(img, null, metadata);
                
                //writer.write(null, iioimage, param);                
                writer.write(metadata, iimage, param);
                streamOut.flush();
                streamOut.close();
                writer.dispose();
                stream.close();
            }
        }
        catch (Exception e) {

            e.printStackTrace();
        }
    }

    static void showMetadata(org.w3c.dom.Node root) {
        try {
        	StringWriter writer = new StringWriter();
			printXml(root, writer);
	    	System.out.println("XML IN String format is: \n" + writer.toString());    	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    static void printXml(Node node, StringWriter writer) throws TransformerException {
    	DOMSource domSource = new DOMSource(node);
    	StreamResult result = new StreamResult(writer);
    	TransformerFactory tf = TransformerFactory.newInstance();
    	Transformer transformer = tf.newTransformer();
    	transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    	transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    	transformer.transform(domSource, result);
    }

}
