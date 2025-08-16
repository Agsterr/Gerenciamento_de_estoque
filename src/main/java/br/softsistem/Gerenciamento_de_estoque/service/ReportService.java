package br.softsistem.Gerenciamento_de_estoque.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Collection;
import java.util.Map;

@Service
public class ReportService {

    private final DataSource dataSource; // opcional (JDBC)

    @Autowired
    public ReportService(@Autowired(required = false) DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public byte[] gerarPdfComBeans(String jrxmlPath, Map<String, Object> params, Collection<?> dados) throws JRException, IOException {
        JasperReport jasper = compilar(jrxmlPath);
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(dados);
        JasperPrint print = JasperFillManager.fillReport(jasper, params, ds);
        return JasperExportManager.exportReportToPdf(print);
    }

    public byte[] gerarPdfComJdbc(String jrxmlPath, Map<String, Object> params) throws Exception {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource não configurado para relatórios via JDBC");
        }
        JasperReport jasper = compilar(jrxmlPath);
        try (Connection conn = dataSource.getConnection()) {
            JasperPrint print = JasperFillManager.fillReport(jasper, params, conn);
            return JasperExportManager.exportReportToPdf(print);
        }
    }

    public byte[] gerarXlsx(String jrxmlPath, Map<String, Object> params, Collection<?> dados) throws JRException, IOException {
        JasperReport jasper = compilar(jrxmlPath);
        JasperPrint print = JasperFillManager.fillReport(jasper, params, new JRBeanCollectionDataSource(dados));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        exporter.exportReport();
        return out.toByteArray();
    }

    // Novo: gerar PDF em branco (ou somente com mensagem) quando não há dados
    public byte[] gerarPdfEmBranco(String titulo, String mensagem) throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("blank-report");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(555);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(20);
        design.setBottomMargin(20);

        // Title band
        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(60);
        JRDesignStaticText titleText = new JRDesignStaticText();
        titleText.setX(0); titleText.setY(0); titleText.setWidth(555); titleText.setHeight(25);
        titleText.setText(titulo != null ? titulo : "Relatório");
        titleBand.addElement(titleText);

        if (mensagem != null && !mensagem.isBlank()) {
            JRDesignStaticText msg = new JRDesignStaticText();
            msg.setX(0); msg.setY(30); msg.setWidth(555); msg.setHeight(20);
            msg.setText(mensagem);
            titleBand.addElement(msg);
        }
        design.setTitle(titleBand);

        JasperReport jasper = JasperCompileManager.compileReport(design);
        JasperPrint print = JasperFillManager.fillReport(jasper, null, new JREmptyDataSource(1));
        return JasperExportManager.exportReportToPdf(print);
    }

    // Novo método para XLSX em branco
    public byte[] gerarXlsxEmBranco(String titulo, String mensagem) throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("blank-xlsx-report");
        design.setPageWidth(595);
        design.setPageHeight(842);
        design.setColumnWidth(555);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(20);
        design.setBottomMargin(20);

        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(60);
        JRDesignStaticText titleText = new JRDesignStaticText();
        titleText.setX(0); titleText.setY(0); titleText.setWidth(555); titleText.setHeight(25);
        titleText.setText(titulo != null ? titulo : "Relatório");
        titleBand.addElement(titleText);
        if (mensagem != null && !mensagem.isBlank()) {
            JRDesignStaticText msg = new JRDesignStaticText();
            msg.setX(0); msg.setY(30); msg.setWidth(555); msg.setHeight(20);
            msg.setText(mensagem);
            titleBand.addElement(msg);
        }
        design.setTitle(titleBand);

        JasperReport jasper = JasperCompileManager.compileReport(design);
        JasperPrint print = JasperFillManager.fillReport(jasper, null, new JREmptyDataSource(1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        exporter.exportReport();
        return out.toByteArray();
    }

    private JasperReport compilar(String jrxmlPath) throws JRException, IOException {
        try (InputStream in = getClass().getResourceAsStream(jrxmlPath)) {
            if (in == null) {
                throw new IOException("JRXML não encontrado no caminho: " + jrxmlPath);
            }
            JasperDesign design = JRXmlLoader.load(in);
            return JasperCompileManager.compileReport(design);
        }
    }
}
