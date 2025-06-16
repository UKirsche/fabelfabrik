package com.fabelfabrik.resource;

import com.fabelfabrik.utils.FileStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.File;

@Path("/api/pdfs")
public class PdfResource {

    private static final Logger LOG = Logger.getLogger(PdfResource.class);

    @Inject
    FileStorageService fileStorageService;

    @GET
    @Path("/{pdfPath: .+}")
    public Response getPdf(@PathParam("pdfPath") String pdfPath) {
        LOG.infof("Retrieving PDF: %s", pdfPath);
        File file = fileStorageService.getPdf(pdfPath);
        if (file == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(file, "application/pdf").build();
    }
}