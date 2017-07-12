import groovy.json.JsonSlurper

/**
 * Created by Uros Gligovic on 12.7.2017..
 */

def text = '''{"widget": {
    "debug": "on",
    "window": {
        "title": "Sample Json",
        "name": "main_window",
        "width": 500,
        "height": 500
    },
    "image": { 
        "src": "Images/IMG.png",
        "name": "sun1",
        "hOffset": 250,
        "vOffset": 250,
        "alignment": "center"
    },
    "text": {
        "data": "Click Here",
        "size": 36,
        "style": "bold",
        "name": "text1",
        "hOffset": 250,
        "vOffset": 100,
        "alignment": "center",
        "onMouseUp": "IMG.opacity = (IMG.opacity / 100) * 90;"
    }
    
}}  

'''
def dateFormat = 'yyyy-MM-dd'

generate(text,"TopLevel",dateFormat,"subFolder/sun/subsub")

def generate(String text, String topLevelName, dateFormat, rootFolder) {
    def jsonSlurper = new JsonSlurper()
    def object
    try {
        object = jsonSlurper.parseText(text)
    } catch (exc) {
        print "ERROR: INVALID JSON"
        return;
    }

    def filesThatWillBeGenerated = new HashSet<String>()
    def filesThatWouldBeOverriden = new ArrayList<String>()
    filesThatWillBeGenerated.add(rootFolder+"/"+topLevelName.capitalize()+'.java')
    getClassesThatWillBeGenerated(object, filesThatWillBeGenerated,rootFolder)
    def folderMakerHelper = ""
    rootFolder.split("/").each {new File(folderMakerHelper+=it+"/").mkdir()}
    def currentDir = new File(rootFolder)
    currentDir.eachFile { x -> if (filesThatWillBeGenerated.contains(x.toString().substring(2))) filesThatWouldBeOverriden.add(x) }

//if (filesThatWouldBeOverriden.size() != 0) {
//    print "WARNING: following files would be overriden " + filesThatWouldBeOverriden + "\n"
//    print "Please remove those files from the chosen folder"
//    return
//}

    def f = new File(rootFolder+"/"+topLevelName.capitalize()+'.java')
    f.write("class " + topLevelName.capitalize()+"{\n")

    generateClass(object, f, dateFormat, topLevelName.capitalize(),rootFolder)
    print "finished"
}

static Object generateClass(object, file, dateFormat, name, String rootFolder) {

    if (object.class == ArrayList.class)
        return generateClass(object.get(0), file, dateFormat, name, rootFolder)

    def temp;
    try {
        for (key in object.keySet()) {


            temp = object.getAt(key)
            // print  object.getAt(obj)
            if (temp.class == object.class) {

                print(key.capitalize() + " " + key)
                file.append("\tprivate " + key.capitalize() + " " + key + ";\n")
                println ""
                def f = new File(rootFolder+"/"+key.capitalize() + '.java')
                f.write("class " + key.capitalize() + "{\n")
                generateClass(temp, f, dateFormat, key,rootFolder)


            } else if (temp.class == ArrayList.class) {
                if (temp.isEmpty()) {
                    println("java.util.List" + "<" + "Object" + "> " + key)
                    file.append("\tprivate " + "java.util.List" + "<" + "Object" + "> " + key + ";\n")

                } else if (temp.getAt(0).class == object.class) {
                    println(temp.class.toString().substring(6) + "<" + key.capitalize() + "> " + key)
                    file.append("\tprivate " + "java.util.List" + "<" + key.capitalize() + "> " + key + ";\n")
                    def f = new File(rootFolder+"/"+key.capitalize() + '.java')
                    f.write("class " + key.capitalize() + "{\n")
                    generateClass(temp.getAt(0), f, dateFormat, key,rootFolder)
                } else {
                    println(temp.class.toString().substring(6) + "<" + temp.getAt(0).class.toString().substring(6) + "> " + key)
                    file.append("\tprivate " + "java.util.List" + "<" + temp.getAt(0).class.toString().substring(6) + "> " + key + ";\n")
                }

            } else {

                try {
                    temp = Date.parse(dateFormat, temp)
                } catch (exc) {
                }
                println(temp.class.toString().substring(6) + " " + key)
                file.append("\tprivate " + temp.class.toString().substring(6) + " " + key + ";\n")

            }


        }
    } catch (exception) {
        println(exception)
    }
    println("")
    makeConstructor(file, name)
    makeGetters(file, object, dateFormat)
    makeSetters(file, object, dateFormat)



    file.append("\n}") //end of a class
    println "++++++++++++++++++++++++++++++++++++++"
    println(object.keySet())
    println "++++++++++++++++++++++++++++++++++++++"

    println("------------------")

}


static void getClassesThatWillBeGenerated(object, HashSet<String> listOfFiles,rootFolder) {

    def temp;
    try {
        for (key in object.keySet()) {


            temp = object.getAt(key)
            // print  object.getAt(obj)
            if (temp.class == object.class) {
                listOfFiles.add(rootFolder+"/"+key.capitalize() + '.java')
                getClassesThatWillBeGenerated(temp, listOfFiles,rootFolder)
            } else if (temp.class == ArrayList.class && temp.getAt(0).class == object.class) {

                listOfFiles.add(rootFolder+"/"+key.capitalize() + '.java')
                getClassesThatWillBeGenerated(temp.getAt(0), listOfFiles,rootFolder)

            }


        }
    } catch (exception) {
        println(exception)
    }

}

static void makeConstructor(File file, String key) {

    file.append("\n\tpublic " + key.capitalize() + "(){" + "}\n")
}

static void makeGetters(File file, object, dateFormat) {

    object.keySet().forEach({ x -> makeAGetter(file, x, object, dateFormat) })
}

static void makeSetters(File file, object, dateFormat) {

    object.keySet().forEach({ x -> makeASetter(file, x, object, dateFormat) })
}

static void makeAGetter(File file, String key, object, String dateFormat) {

    if (object.getAt(key).class == object.class) {

        file.append("\n\tpublic " + key.capitalize() + " get" + key.capitalize() + "(){\n \t\t " + "return this." + key + ";\n" + "\t}\n \n")

    } else if (object.getAt(key).class == ArrayList.class) {

        if (object.getAt(key).isEmpty()) {
            file.append("\n\tpublic " + "java.util.List<" + "Object" + ">" + " get" + key.capitalize() + "(){\n  " + "\t\treturn this." + key + ";\n" + "\t}\n \n")

        } else if (object.getAt(key).get(0).class == object.class) {
            file.append("\n\tpublic " + "java.util.List<" + key.capitalize() + ">" + " get" + key.capitalize() + "(){\n  " + "\t\treturn this." + key + ";\n" + "\t}\n \n")

        } else {
            file.append("\n\tpublic " + "java.util.List<" + object.getAt(key).get(0).class.toString().substring(6) + ">" + " get" + key.capitalize() + "(){\n  " + "\t\treturn this." + key + ";\n" + "\t}\n \n")

        }
    } else {

        def temp = object.getAt(key)
        try {
            temp = Date.parse(dateFormat, temp)
        } catch (exc) {
        }
        file.append("\n\tpublic " + temp.class.toString().substring(6) + " get" + key.capitalize() + "(){\n  " + "\t\treturn this." + key + ";\n" + "\t}\n \n")

    }
}

static void makeASetter(File file, String key, object, String dateFormat) {

    if (object.getAt(key).class == object.class) {

        file.append("\n\tpublic " + "void" + " set" + key.capitalize() + "(" + key.capitalize() + " " + key + "){\n  " + "\t\tthis." + key + " = " + key + ";\n" + "\t}\n \n")

    } else if (object.getAt(key).class == ArrayList.class) {
        if (object.getAt(key).isEmpty()) {
            file.append("\n\tpublic " + "void" + " set" + key.capitalize() + "(" + "java.util.List<" + "Object" + ">" + " " + key + "){\n  " + "\t\tthis." + key + " = " + key + ";\n" + "\t}\n \n")

        } else if (object.getAt(key).get(0).class == object.class) {
            file.append("\n\tpublic " + "void" + " set" + key.capitalize() + "(" + "java.util.List<" + key.capitalize() + ">" + " " + key + "){\n  " + "\t\tthis." + key + " = " + key + ";\n" + "\t}\n \n")
        } else {
            file.append("\n\tpublic " + "void" + " set" + key.capitalize() + "(" + "java.util.List<" + object.getAt(key).get(0).class.toString().substring(6) + ">" + " " + key + "){\n  " + "\t\tthis." + key + " = " + key + ";\n" + "\t}\n \n")

        }
    } else {

        def temp = object.getAt(key)
        try {
            temp = Date.parse(dateFormat, temp)
        } catch (exc) {
        }
        file.append("\n\tpublic " + "void" + " set" + key.capitalize() + "(" + temp.class.toString().substring(6) + " " + key + "){\n  " + "\t\tthis." + key + " = " + key + ";\n" + "\t}\n \n")

    }
}
