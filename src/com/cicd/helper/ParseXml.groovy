package com.cicd.helper

def parseIt(Map args =[ xmlPath: "C:"]){
    String xml_path = args.xmlPath.toString()

    def xmlFile = getClass().getResourceAsStream("@" + xml_path)

    def articles = new XmlParser().parse(xmlFile)

    bat(script: "echo " + articles.'*'.size())
}

//Name change

def sh(args){
    return sh(args.script)
}