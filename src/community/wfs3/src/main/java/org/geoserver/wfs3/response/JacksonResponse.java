/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.response.WFSResponse;
import org.geoserver.wfs3.BaseRequest;

/** Response encoding outputs in JSON/YAML using Jackson */
public abstract class JacksonResponse extends WFSResponse {

    public JacksonResponse(GeoServer gs, Class targetClass) {
        super(
                gs,
                targetClass,
                new LinkedHashSet<>(
                        Arrays.asList(
                                BaseRequest.JSON_MIME,
                                BaseRequest.YAML_MIME,
                                BaseRequest.XML_MIME)));
    }

    @Override
    public boolean canHandle(Operation operation) {
        return isJsonFormat(operation) || isYamlFormat(operation) || isXMLFormat(operation);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (isJsonFormat(operation)) {
            return BaseRequest.JSON_MIME;
        } else if (isYamlFormat(operation)) {
            return BaseRequest.YAML_MIME;
        } else if (isXMLFormat(operation)) {
            return BaseRequest.XML_MIME;
        } else {
            throw new ServiceException("Unknown format requested " + getFormat(operation));
        }
    }

    private String getFormat(Operation operation) {
        BaseRequest request = (BaseRequest) operation.getParameters()[0];
        Optional<String> format = Optional.ofNullable(request.getOutputFormat());
        return format.orElse(BaseRequest.JSON_MIME);
    }

    private boolean isJsonFormat(Operation operation) {
        return BaseRequest.JSON_MIME.equalsIgnoreCase(getFormat(operation));
    }

    private boolean isYamlFormat(Operation operation) {
        return BaseRequest.YAML_MIME.equalsIgnoreCase(getFormat(operation));
    }

    private boolean isXMLFormat(Operation operation) {
        return BaseRequest.XML_MIME.equalsIgnoreCase(getFormat(operation));
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        ObjectMapper mapper;
        if (isYamlFormat(operation)) {
            YAMLFactory factory = new YAMLFactory();
            mapper = new ObjectMapper(factory);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        } else if (isXMLFormat(operation)) {
            mapper = new XmlMapper();
            // using a custom annotation introspector to set the desired namespace
            mapper.setAnnotationIntrospector(
                    new JacksonXmlAnnotationIntrospector() {
                        @Override
                        public String findNamespace(Annotated ann) {
                            String ns = super.findNamespace(ann);
                            if (ns == null || ns.isEmpty()) {
                                return "http://www.opengis.net/wfs/3.0";
                            } else {
                                return ns;
                            }
                        }
                    });

        } else {
            mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        mapper.writeValue(output, value);
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return getFileName(value, operation) + (isJsonFormat(operation) ? ".json" : ".yaml");
    }

    /**
     * Just the name of the file to be returned (no extension)
     *
     * @return
     */
    protected abstract String getFileName(Object value, Operation operation);
}
