import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


import dao.Doc;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProcessMht {

    public static List<Doc> arrayList = new ArrayList<Doc>();



    public static void getFile(String filepath) throws IOException, MessagingException {
        System.out.println(filepath);
        File file = new File(filepath);
        if(!file.isDirectory()){
            String strText = null;
            FileInputStream fileInput = new FileInputStream(file);
            Session mailSession = Session.getDefaultInstance(System.getProperties(), null);
            MimeMessage msg = null;
            msg = new MimeMessage(mailSession, fileInput);
            Object obcontent = msg.getContent();
            if (obcontent instanceof Multipart) {
                MimeMultipart mp = (MimeMultipart) obcontent;
                MimeBodyPart bp1 = (MimeBodyPart) mp.getBodyPart(0);
                String strEncodng = "UTF-8";
                strText = getMhtText(bp1, strEncodng);
            }

            Document docJsoup = Jsoup.parse(strText);// 解析HTML字符串返回一个Document实现
            String title = docJsoup.select("h2").first().text();
            String content = docJsoup.select("p").text();
            Doc oneDoc = new Doc();
            oneDoc.setTitle(title);
            oneDoc.setContent(content);
            arrayList.add(oneDoc);
        }else {
            String[] fileList = file.list();
            for (String onefile : fileList){
                getFile(filepath+"\\"+onefile);
            }
        }

    }

    private static String getMhtText(MimeBodyPart bp, String strEncoding) throws IOException, MessagingException {
        InputStream textStream = bp.getInputStream();
        BufferedInputStream buff = new BufferedInputStream(textStream);
        Reader r = new InputStreamReader(buff, strEncoding);
        BufferedReader br = new BufferedReader(r);
        StringBuffer strHtml = new StringBuffer("");
        String line;
        while ((line = br.readLine()) != null) {
            strHtml.append(line+"\r\n");
        }
        br.close();
        r.close();
        textStream.close();
        return strHtml.toString();
    }


}


//        List<Doc> arrayList = new ArrayList<Doc>();
//        FileInputStream file = new FileInputStream("D:\\data\\test1\\test.txt");
//        InputStreamReader fileReader = new InputStreamReader(file, "UTF-8");
//        BufferedReader br = new BufferedReader(fileReader);
//        String line = null;
//        while ((line = br.readLine()) != null) {
//            Doc docline= new Doc();
//            String[] linesplit = line.split("\t");
//            // System.out.print("输出记录id"+linesplit[0]);
//            int parseid = Integer.parseInt(linesplit[0].trim());
//            docline.setId(parseid);
//            docline.setTitle(linesplit[1]);
//            docline.setAuthor(linesplit[2]);
//            docline.setDescribe(linesplit[3]);
//            docline.setContent(linesplit[3]);
//            arrayList.add(docline);
//        }
//        return arrayList;
