//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;

public class Emails {
    public Emails() {
    }

    public static void send(String toEmail, String subject, String content) throws IOException {
        Email from = new Email("kevin.li.20742@utexas.edu");
        Email to = new Email(toEmail);
        Content c = new Content("text/plain", content);
        Mail mail = new Mail(from, subject, to, c);
        SendGrid sg = new SendGrid("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");
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
        } catch (IOException var10) {
            throw var10;
        }
    }
}
