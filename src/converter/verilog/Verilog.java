package converter.verilog;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import common.FileList;
import common.MyDebug;

import parser.IParser;
import parser.ParserException;
import parser.verilog.LibraryManager;
import parser.verilog.VerilogParser;
import converter.hdlConverter;
import converter.verilog.ScSource_text;

public class Verilog extends hdlConverter
{
static final int MAX_FILE_SIZE = 500*1024;
    
    @Override
    public void convertFile(String srcPath, String dstPath)
            throws ParserException, IOException
    {
        MyDebug.printFileLine("convertFile:" + srcPath);
        VerilogParser parser = new VerilogParser(false);
        parser.parse(srcPath);
        ScSource_text root = null;
        
        File file = new File(srcPath);
        
        String fileName = file.getName();
        String fileNameWithoutExtention = fileName.substring(0, fileName.lastIndexOf('.'));
        String fileDestParh = dstPath  + File.separatorChar + fileNameWithoutExtention;
        
        if(file.length() > MAX_FILE_SIZE) {
            root = new ScSource_text(parser, true);
            saveAllDividual(root, fileDestParh);
        }else {
            root = new ScSource_text(parser);
            saveHeader(root, fileDestParh);
            saveSource(root, fileDestParh);
        }
    }
    
    private String preHeaderGuard(String name)
    {
        String ret = "";
        name = name.toUpperCase();
        name = "__" + name.replace('.', '_') + "__";
        ret += "#ifndef " + name + System.lineSeparator();
        ret += "#define " + name;
        return ret;
    }
    
    private String postHeaderGuard(String name)
    {
        String ret = "";
        name = name.toUpperCase();
        name = "__" + name.replace('.', '_') + "__";
        ret += "#endif  // " + "#ifndef " + name;
        return ret;
    }
    
    private void saveAllDividual(ScSource_text root, String dstPath)
    {
        String hPath = dstPath+".h";
        hPath = hPath.replace('\\', '/');
        int index = hPath.lastIndexOf('/');
        String name = "";
        if(index > 0)
            name = hPath.substring(index+1);
        else
            name = hPath;
        
        try {
            createFile(hPath, true);
            PrintStream hFileBuff = new PrintStream(hPath);
            
            StringBuffer strInclude = new StringBuffer();
            StringBuffer strDeclaration = new StringBuffer();
            StringBuffer strImplements = new StringBuffer();
            
            root.getIndividualString(strInclude, strDeclaration, strImplements);
            
            hFileBuff.println(preHeaderGuard(name) + System.lineSeparator());
            hFileBuff.println(strInclude);
            hFileBuff.print(strDeclaration);
            hFileBuff.println(postHeaderGuard(name));
            hFileBuff.close();
            
            if(strImplements.length() > 0) {
                String sPath = dstPath+".cpp";
                createFile(sPath, true);
                PrintStream sFileBuff = new PrintStream(sPath);
                sFileBuff.println(System.lineSeparator() + "#include \"" + name + "\"" + System.lineSeparator());
                sFileBuff.print(strImplements);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveHeader(ScSource_text root, String dstPath)
    {
        String path = dstPath+".h";
        path = path.replace('\\', '/');
        int index = path.lastIndexOf('/');
        String name = "";
        if(index > 0)
            name = path.substring(index+1);
        else
            name = path;
        try {
            createFile(path, true);
            PrintStream fileBuff = new PrintStream(path);
            fileBuff.println(preHeaderGuard(name));
            fileBuff.println(root.getInclude());
            fileBuff.print(root.getDeclaration());
            fileBuff.println(postHeaderGuard(name));
            fileBuff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void saveSource(ScSource_text root, String dstPath)
    {
        try {
            String buff = root.getImplements();
            //if(buff.isEmpty())
            //    return;
            createFile(dstPath+".cpp", true);
            PrintStream fileBuff = new PrintStream(dstPath+".cpp");
            String path = dstPath+".h";
            path = path.replace('\\', '/');
            int index = path.lastIndexOf('/');
            String name = "";
            if(index > 0)
                name = path.substring(index+1);
            else
                name = path;
            fileBuff.println(System.lineSeparator() + "#include \"" + name + "\"" + System.lineSeparator());
            fileBuff.print(buff);
            fileBuff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void convertDir(String srcDir, String dstDir)
    {
        FileList list = new FileList(srcDir, IParser.EXT_VERILOG);
        srcDir = srcDir.toLowerCase();
        dstDir = dstDir.toLowerCase();
        for(int i = 0; i < list.getFileNum(); i++) {
            String filePath = list.getFile(i);
            filePath = filePath.toLowerCase();
            int index = filePath.lastIndexOf(IParser.EXT_VERILOG);
            int index2 = filePath.indexOf(srcDir);
            String name = filePath;
            if(index >= 0 && index2 >= 0) {
                index2 += srcDir.length();
                name = filePath.substring(index2+1, index-1);
            }
            String dstPath = dstDir + File.pathSeparator + name;
            try {
                convertFile(filePath, dstPath);
            } catch (Exception e) {
                StackTraceElement[] stackEle = e.getStackTrace();
                System.err.println("stackEle.length:" + stackEle.length);
                if(stackEle.length > 7) {
                    MyDebug.printStackTrace(e);
                    //e.printStackTrace();
                }
            }
            System.gc();    // force garbage collection, release memory
        }
    }

    public void addLibary(String srcDir, String libName)
    {
        LibraryManager.getInstance().addDir(srcDir);
    }
}
