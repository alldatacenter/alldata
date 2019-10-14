package top.omooo.tinypng_plugin

import com.tinify.Tinify
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.security.MessageDigest
import java.text.DecimalFormat

class TinyPngTask extends DefaultTask {

    TinyPngExtension mTinyPngExtension

    TinyPngTask() {
        mTinyPngExtension = project.tinyInfo
    }

    @TaskAction
    void run() {
        println("Task run~")
        println(mTinyPngExtension.toString())
        if (mTinyPngExtension.resourceDir == []) {
            println('TinyTask mast have resourceDir!')
            return
        }
        if (mTinyPngExtension.apiKey == null) {
            println('TinyTask mast have apiKey!')
            return
        }
        try {
            Tinify.setKey(mTinyPngExtension.apiKey)
            Tinify.validate()
        } catch (Exception e) {
            println('TinyPng apiKey is not available!')
            e.printStackTrace()
            return
        }
        def compressedList = new ArrayList<TinyPngInfo>()
        def compressedListFile = new File("${project.projectDir}/compressed-resource.json")
        println("${project.projectDir}/compressed-resource.json")

        if (!compressedListFile.exists()) {
            compressedListFile.createNewFile()
        } else {
            try {
                def list = new JsonSlurper().parse(compressedListFile, "utf-8")
                if (list instanceof ArrayList) {
                    compressedList = list
                } else {
                    println('compressed-resource.json is not available!')
                }
            } catch (Exception e1) {
                println('compressed-resource.json is not available!')
                e1.printStackTrace()
            }
        }

        def beforeSize = 0L
        def afterSize = 0L
        def error = false
        def newCompressedList = new ArrayList<TinyPngInfo>()

        mTinyPngExtension.resourceDir.each { d ->
            def dir = new File(d)
            if (dir.exists() && dir.isDirectory()) {
                if (mTinyPngExtension.resourcePattern == []) {
                    mTinyPngExtension.resourcePattern = ["drawable[a-z-]*"]
                }
                mTinyPngExtension.resourcePattern.each { p ->
                    dir.eachDirMatch(~/$p/) {
                        drawDir ->
                            if (!error) {
                                TinyPngResult result = compress(drawDir, mTinyPngExtension.whiteList, compressedList)
                                beforeSize += result.beforeSize
                                afterSize += result.afterSize
                                error = result.error
                                if (result.getResults()) {
                                    newCompressedList.addAll(result.getResults())
                                }
                            }
                    }
                }
            }
        }
        if (newCompressedList) {
            for (TinyPngInfo tinyPngInfo : newCompressedList) {
                def index = compressedList.path.indexOf(tinyPngInfo.path)
                if (index >= 0) {
                    compressedList[index] = tinyPngInfo
                } else {
                    compressedList.add(0, tinyPngInfo)
                }
            }
            def jsonOutput = new JsonOutput()
            def json = jsonOutput.toJson(compressedList)
            compressedListFile.write(jsonOutput.prettyPrint(json), "utf-8")
            println("//*************//")
            println("Task finish，compress ${newCompressedList.size()} files")
            println("Before total size: ${beforeSize}")
            println("After total size: ${afterSize}")
            println("//*************//")
        }
    }

    static TinyPngResult compress(File resDir, Iterable<String> whiteList, Iterable<TinyPngInfo> compressedList) {
        def newCompressedList = new ArrayList<TinyPngInfo>()
        def accountError = false
        def beforeTotalSize = 0
        def afterTotalSize = 0
        label:
        for (File file : resDir.listFiles()) {

            def filePath = file.path
            def fileName = file.name

            for (String s : whiteList) {
                if (fileName == ~/$s/) {
                    println("Match white list,skip it $filePath")
                    continue label
                }
            }

            for (TinyPngInfo info : compressedList) {
                if (filePath == info.path && generateMD5(file) == info.md5) {
                    continue label
                }
            }

            if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                if (fileName.contains(".9")) {
                    continue
                }

                println("Find target pic : $filePath")

                def fis = new FileInputStream(file)

                try {
                    def beforeSize = fis.available()
                    def beforeSizeStr = formetFileSize(beforeSize)

                    def tSource = Tinify.fromFile("${resDir}/${fileName}")
                    tSource.toFile("${resDir}/${fileName}")

                    def afterSize = fis.available()
                    def afterSizeStr = formetFileSize(afterSize)

                    beforeTotalSize += beforeSize
                    afterTotalSize += afterSize
                    newCompressedList.add(new TinyPngInfo(filePath, beforeSizeStr, afterSizeStr, generateMD5(file)))

                    println("BeforeSize: $beforeSizeStr -> AfterSize: ${afterSizeStr}")
                } catch (Exception e) {
                    e.printStackTrace()
                }
            }
        }
        return new TinyPngResult(beforeTotalSize, afterTotalSize, accountError, newCompressedList)
    }

    static String generateMD5(File file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.withInputStream() { is ->
            int read
            byte[] buffer = new byte[8192]
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read)
            }
        }
        byte[] md5Sum = digest.digest()
        BigInteger bigInteger = new BigInteger(1, md5Sum)
        return bigInteger.toString(16).padLeft(32, '0')
    }

    static String formetFileSize(long fileS) {
        def df = new DecimalFormat("#.00")
        if (fileS == 0L) {
            return "0B"
        }
        if (fileS < 1024) {
            return df.format((double) fileS) + "B"
        } else if (fileS < 1048576) {
            return df.format((double) fileS / 1024) + "KB"
        } else if (fileS < 1073741824) {
            return df.format((double) fileS / 1048576) + "MB"
        } else {
            return df.format((double) fileS / 1073741824) + "GB"
        }
    }
}