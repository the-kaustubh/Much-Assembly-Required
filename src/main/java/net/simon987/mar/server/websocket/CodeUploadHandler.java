package net.simon987.mar.server.websocket;

import net.simon987.mar.server.GameServer;
import net.simon987.mar.server.assembly.Assembler;
import net.simon987.mar.server.assembly.AssemblyResult;
import net.simon987.mar.server.assembly.CPU;
import net.simon987.mar.server.logging.LogManager;
import org.json.simple.JSONObject;

import java.io.IOException;

public class CodeUploadHandler implements MessageHandler {

    @Override
    public void handle(OnlineUser user, JSONObject json) throws IOException {
        if (json.get("t").equals("uploadCode")) {

            LogManager.LOGGER.fine("(WS) Code upload from " + user.getUser().getUsername());

            if (!user.getUser().isGuest()) {
                //TODO Should we wait at the end of the tick to modify the CPU ?
                user.getUser().setUserCode((String) json.get("code"));

                if (user.getUser().getUserCode() != null) {

                    CPU cpu = user.getUser().getControlledUnit().getCpu();

                    AssemblyResult ar = new Assembler(cpu.getInstructionSet(),
                            cpu.getRegisterSet(),
                            GameServer.INSTANCE.getConfig()).parse(user.getUser().getUserCode());

                    user.getUser().setCodeLineMap(ar.codeLineMap);
                    user.getUser().setDisassembly(ar.disassemblyLines);

                    cpu.getMemory().clear();

                    //Write assembled code to mem
                    char[] assembledCode = ar.getWords();

                    cpu.getMemory().write((char) ar.origin, assembledCode, 0, assembledCode.length);
                    cpu.setCodeSectionOffset(ar.getCodeSectionOffset());

                    cpu.reset();
                    cpu.getRegisterSet().clear();
                    //Clear keyboard buffer
                    if (user.getUser().getControlledUnit() != null &&
                            user.getUser().getControlledUnit().getKeyboardBuffer() != null) {
                        user.getUser().getControlledUnit().getKeyboardBuffer().clear();
                    }

                    JSONObject response = new JSONObject();
                    response.put("t", "codeResponse");
                    response.put("bytes", ar.bytes.length);
                    response.put("exceptions", ar.exceptions.size());

                    user.getWebSocket().getRemote().sendString(response.toJSONString());
                }
            }
        }
    }
}
