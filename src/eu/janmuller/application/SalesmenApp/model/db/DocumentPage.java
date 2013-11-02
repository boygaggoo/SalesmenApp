package eu.janmuller.application.salesmenapp.model.db;

import eu.janmuller.android.dao.api.GenericModel;
import eu.janmuller.android.dao.api.Id;
import eu.janmuller.application.salesmenapp.IHideAble;

/**
 * Created with IntelliJ IDEA.
 * Coder: Jan Müller
 * Date: 01.11.13
 * Time: 15:20
 */
@GenericModel.TableName(name = "document_pages")
@GenericModel.IdType(type = GenericModel.IdTypeEnum.LONG)
final public class DocumentPage extends Page<DocumentPage> implements IHideAble {

    @ForeignKey(attributeClass = Document.class)
    public Id documentId;

    @GenericModel.DataType(type = DataTypeEnum.BOOLEAN)
    public boolean show;

    /**
     * Implicitni konstruktor
     */
    public DocumentPage() {

    }

    public DocumentPage(TemplatePage templatePage, Document document) {

        super(templatePage);
        this.documentId = document.id;
        this.show = true;
    }

    @Override
    public void setVisibility(boolean visible) {

        show = visible;
    }

    @Override
    public boolean isVisible() {

        return show;
    }
}
