package com.polafacebook.process.engine.machine.controller;

import com.polafacebook.model.Context;
import com.polafacebook.polapi.Pola;
import com.polafacebook.polapi.Result;
import com.polafacebook.ports.outgoing.OnNewOutgoingMessageListener;
import com.polafacebook.process.engine.machine.MachineState;
import com.polafacebook.process.engine.message.OutgoingMessage;
import com.polafacebook.process.service.BarCodeService;

import java.io.IOException;

public class ProductController {
    public static final char CHR_CHECK_MARK = (char) 0x2713;
    public static final char CHR_EX_MARK = (char) 0x2717;

    private final OnNewOutgoingMessageListener listener;
    private BarCodeService barCodeService;
    private Pola polaService;

    public ProductController(OnNewOutgoingMessageListener listener, BarCodeService barCodeService, Pola polaService) {
        this.listener = listener;
        this.barCodeService = barCodeService;
        this.polaService = polaService;
    }

    public void setBarCodeService(BarCodeService barCodeService) {
        this.barCodeService = barCodeService;
    }

    public void setPolaService(Pola polaService) {
        this.polaService = polaService;
    }

    public MachineState onImage(MachineState from, MachineState to, Context context) {
        listener.onNewMessage(new OutgoingMessage("Przetwarzam obrazek. ", context.userId));
        String code;
        try {
            code = barCodeService.processBarCode(context.lastAttachment.getInputStream());
            if (code == null) {
                return MachineState.NOT_RECOGNIZED;
            }
            listener.onNewMessage(new OutgoingMessage("Znaleziono kod: " + code, context.userId));
        } catch (IOException e) {
            e.printStackTrace();
            return MachineState.NOT_RECOGNIZED;
        }

        try {
            return queryDB(code, context);
        } catch (IOException e) {
            e.printStackTrace();
            return handleDBError(context);
        }
    }

    public MachineState onText(MachineState from, MachineState to, Context context) {
        listener.onNewMessage(new OutgoingMessage("Kod produktu otrzymany: " + context.eanCode, context.userId));
        try {
            return queryDB(context.eanCode, context);
        } catch (IOException e) {
            e.printStackTrace();
            return handleDBError(context);
        }
    }

    public MachineState onNotRecognized(MachineState from, MachineState to, Context context) {
        OutgoingMessage outgoingMessage = new OutgoingMessage("Nie udało nam się pobrać kodu z obrazka. Możesz spróbować wpisać kod ręcznie lub wysłać lepsze zdjęcie.", context.userId);

        outgoingMessage.addQuickReply("Pomoc", "HELP");
        outgoingMessage.addQuickReply("Informacje", "INFO");
        outgoingMessage.addQuickReply("Metodyka", "METHODOLOGY");

        listener.onNewMessage(outgoingMessage);
        return MachineState.WAIT_FOR_ACTION;
    }

    public MachineState onDisplayResults(MachineState from, MachineState to, Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ocena: ").append(context.result.getPlScore()).append("/100").append("\n");
        sb.append("Producent: ").append(context.result.getName()).append("\n");
        sb.append("Udział polskiego kapitału: ").append(context.result.getPlCapital()).append("%\n");
        sb.append(context.result.isPlWorkers() ? CHR_CHECK_MARK : CHR_EX_MARK).append(" produkuje w Polsce\n");
        sb.append(context.result.isPlRnD() ? CHR_CHECK_MARK : CHR_EX_MARK).append(" prowadzi badania i rozwój w Polsce\n");
        sb.append(context.result.isPlRegistered() ? CHR_CHECK_MARK : CHR_EX_MARK).append(" zarejestrowana w Polsce\n");
        sb.append(context.result.isPlNotGlobEnt() ? CHR_CHECK_MARK : CHR_EX_MARK).append(" jest członkiem zagranicznego koncernu\n");

        sb.append(context.result.getDescription());

        listener.onNewMessage(new OutgoingMessage(sb.toString(), context.userId));
        return MachineState.ASK_FOR_CHANGES_OR_ACTION;
    }

    private MachineState handleDBError(Context context) {
        listener.onNewMessage(new OutgoingMessage("Mamy usterkę i nie możemy w tym momencie uzyskać informacji na temat tego kodu. Spróbuj ponownie później. Przepraszamy! ", context.userId));
        return MachineState.WELCOME;
    }

    private MachineState queryDB(String code, Context context) throws IOException {
        Result result = polaService.getByCode(code);
        context.result = result;
        if (result.getDescription() != null) {
            return MachineState.DISPLAY_RESULTS;
        } else {
            return MachineState.REPORT_PROMPT_IMAGE;
        }
    }
}
