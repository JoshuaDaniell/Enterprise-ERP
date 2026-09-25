package com.finance.finance_service.Service;
import com.finance.finance_service.Entity.Invoice; import lombok.RequiredArgsConstructor; import org.apache.pdfbox.pdmodel.*; import org.apache.pdfbox.pdmodel.font.PDType1Font; import org.apache.pdfbox.pdmodel.font.Standard14Fonts; import org.apache.pdfbox.pdmodel.common.PDRectangle; import org.apache.pdfbox.pdmodel.PDPageContentStream; import org.springframework.stereotype.Service; import java.io.*; import java.nio.charset.StandardCharsets;
@Service @RequiredArgsConstructor
public class InvoicePdfService {
 public byte[] generate(Invoice i) {
  try(var out=new ByteArrayOutputStream(); var doc=new PDDocument()){var page=new PDPage(PDRectangle.A4);doc.addPage(page);try(var s=new PDPageContentStream(doc,page)){s.beginText();s.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);s.setLeading(18);s.newLineAtOffset(50,750);String[] lines={"INVOICE "+i.getInvoiceNumber(),"Order: "+i.getOrderNumber(),"Customer: "+(i.getCustomerName()==null?"":i.getCustomerName()),"","Subtotal: "+i.getSubtotal(),"Tax: "+i.getTaxAmount(),"Total: "+i.getTotalAmount(),"Payment status: "+i.getPaymentStatus()};for(String l:lines){s.showText(l);s.newLine();}s.endText();}doc.save(out);return out.toByteArray();}catch(IOException e){throw new IllegalStateException("Unable to generate invoice PDF",e);}
 }
}
