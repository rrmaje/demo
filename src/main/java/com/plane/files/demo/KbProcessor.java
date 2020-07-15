package com.plane.files.demo;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class KbProcessor {

final static String[] DELETE_PATTERNS = {"#main-header","#footer",".page-metadata"};

static String processText(Path html) throws IOException{

    return deleteElements(html);
    

}

private static String deleteElements(Path html) throws IOException{

    Document doc = Jsoup.parse(html.toFile(), "UTF-8");

    Stream.of(DELETE_PATTERNS).forEach(s-> doc.select(s).remove());
/*
    try(FileWriter fw = new FileWriter(html.toFile())){
        fw.write(doc.toString());
    }
*/
    return doc.toString();

}



}