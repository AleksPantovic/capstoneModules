package com.asioso.firstspirit.translatormodule.executable;

import com.asioso.firstspirit.translatormodule.controller.TranslateController;
import de.espirit.firstspirit.access.ClientScriptContext;
import de.espirit.firstspirit.access.script.Executable;
import de.espirit.firstspirit.access.store.ElementDeletedException;
import de.espirit.firstspirit.access.store.LockException;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.ui.operations.RequestOperation;

import java.io.Writer;
import java.util.Map;

public class SimpleExecutable implements Executable {

    @Override
    public Object execute(Map<String, Object> map, Writer writer, Writer writer1) {
        ClientScriptContext clientScriptContext = (ClientScriptContext) map.get("context");
        try {
            if(clientScriptContext.getElement() instanceof Page) {
                new TranslateController(clientScriptContext, clientScriptContext.getElement().getId());
            }
            else {
                OperationAgent operationAgent = clientScriptContext.requestSpecialist(OperationAgent.TYPE);
                assert operationAgent != null;
                RequestOperation operation = operationAgent.getOperation(RequestOperation.TYPE);
                assert operation != null;
                operation.perform("The translation can be run just on page level");
            }
        } catch (LockException | ElementDeletedException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
