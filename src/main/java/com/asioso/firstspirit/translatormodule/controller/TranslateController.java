package com.asioso.firstspirit.translatormodule.controller;

import com.deepl.api.DeepLException;
import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import de.espirit.firstspirit.access.BaseContext;
import de.espirit.firstspirit.access.Language;
import de.espirit.firstspirit.access.store.ElementDeletedException;
import de.espirit.firstspirit.access.store.LockException;
import de.espirit.firstspirit.access.store.Store;
import de.espirit.firstspirit.access.store.StoreElement;
import de.espirit.firstspirit.access.store.pagestore.Page;
import de.espirit.firstspirit.access.store.pagestore.Section;
import de.espirit.firstspirit.access.store.templatestore.gom.GomEditorProvider;
import de.espirit.firstspirit.access.store.templatestore.gom.GomFormElement;
import de.espirit.firstspirit.agency.OperationAgent;
import de.espirit.firstspirit.agency.StoreAgent;
import de.espirit.firstspirit.forms.FormData;
import de.espirit.firstspirit.ui.operations.RequestOperation;

public class TranslateController extends BaseController{
    private final long id;
    private final String apiKey = "122b5124-67a6-3882-c6c5-4df7a19b6f6d";

    private final Translator translator = new Translator(apiKey);

    public TranslateController(BaseContext baseContext, long id) throws LockException, ElementDeletedException {
        super(baseContext);
        this.id = id;
        translate();
    }

    private void translate(){
        this.getBaseContext().logInfo("Translation has started...\n");
        StoreAgent storeAgent = this.getBaseContext().requestSpecialist(StoreAgent.TYPE);

        assert storeAgent != null;
        Store store = storeAgent.getStore(Store.Type.PAGESTORE);
        Page page = (Page) store.getStoreElement(this.id);
        assert page != null;
        boolean released = page.isReleased();
        Language masterLanguage = store.getProject().getMasterLanguage();
        FormData formData;

        for (Language language : store.getProject().getLanguages()){
            if(language.toString().equals(masterLanguage.toString())) continue;
            try {
                page.setLock(true);
                formData = page.getFormData();
                for (GomFormElement gomFormElement : formData.getForm().forms()) {
                    final String gomElementTag = gomFormElement.getGomElementTag();

                    if ("CMS_INPUT_TEXT".equals(gomElementTag)) {
                        String value = (String) formData.get(masterLanguage, gomFormElement.name()).get();
                        TextResult translatedText = translator.translateText(value, masterLanguage.toString().toUpperCase(), language.toString().toUpperCase());
                        formData.get(language, gomFormElement.name()).set(translatedText.getText());
                        page.setFormData(formData);
                    }
                }
                page.addTranslated(language);
                page.save();
                if(released) {
                    page.release();
                }
                page.setLock(false);
            } catch (LockException e) {
                showMessage("The page is already locked " + e);
            } catch (ElementDeletedException e) {
                showMessage("The page has been deleted " + e);
            } catch (DeepLException e) {
                showMessage("The error has occurred with the API  " + e);
            } catch (InterruptedException e) {
                showMessage("The API request was interrupted " + e);
            }
        }

        for(StoreElement contentArea:page.getChildren()){
            for(StoreElement sections:contentArea.getChildren()) {
                for (Language language : store.getProject().getLanguages()) {
                    if(language.toString().equals(masterLanguage.toString())) continue;
                    try {
                        page.setLock(true);
                        Section section = (Section) sections;
                        FormData sectionFormData = section.getFormData();//Get form data for the section
                        GomEditorProvider sec = sectionFormData.getForm();
                        Iterable<GomFormElement> inputComponents = sec.forms();
                        inputComponents.forEach(inputComponent -> {
                            String gomElementTag = inputComponent.getGomElementTag();//type of input
                            String fieldName = inputComponent.getName().toString(); //field name
                            if ((gomElementTag.equals("CMS_INPUT_TEXT"))
                                    && sec.findEditor(fieldName).usesLanguages()) {
                                String oldVal = (String) sectionFormData.get(masterLanguage, fieldName).get();
                                if (oldVal != null && !oldVal.isEmpty()) {
                                    this.getBaseContext().logInfo("Translating field: " + fieldName + " with value: " + oldVal);
                                    String value = (String) sectionFormData.get(masterLanguage, inputComponent.name()).get();
                                    TextResult translatedText = null;
                                    try {
                                        translatedText = translator.translateText(value, masterLanguage.toString().toUpperCase(), language.toString().toUpperCase());
                                    } catch (DeepLException e) {
                                        throw new RuntimeException(e);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    sectionFormData.get(language, inputComponent.name()).set(translatedText.getText());
                                    section.setFormData(sectionFormData);
                                    section.save();
                                } else {
                                    this.getBaseContext().logWarning("Field '{}' in master language '{}' is empty or null" + fieldName);
                                }
                            }
                        });
                        page.setLock(false);

                    } catch (LockException e) {
                        throw new RuntimeException(e);
                    } catch (ElementDeletedException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }
        this.getBaseContext().logInfo("Translation has ended...\n");

        showMessage("Operation completed successfully!");
    }

    public void showMessage(String message){
        OperationAgent operationAgent = this.getBaseContext().requestSpecialist(OperationAgent.TYPE);
        assert operationAgent != null;
        RequestOperation operation = operationAgent.getOperation(RequestOperation.TYPE);
        assert operation != null;
        operation.perform(message);
    }
}
