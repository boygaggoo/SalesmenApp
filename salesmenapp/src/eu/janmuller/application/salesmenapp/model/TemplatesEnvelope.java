package eu.janmuller.application.salesmenapp.model;

import com.google.gson.annotations.SerializedName;
import eu.janmuller.application.salesmenapp.model.db.Template;

/**
 * Created with IntelliJ IDEA.
 * Coder: Jan Müller
 * Date: 19.10.13
 * Time: 17:46
 */
public class TemplatesEnvelope {

    @SerializedName("Templates")
    public Template[] templates;
}
