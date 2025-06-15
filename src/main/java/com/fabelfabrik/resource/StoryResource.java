package com.fabelfabrik.resource;

import com.fabelfabrik.model.Story;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/api/stories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StoryResource {

    private static final Logger LOG = Logger.getLogger(StoryResource.class);

    @GET
    public List<Story> getAll() {
        return Story.listAll();
    }

    @POST
    public Story create(Story story) {
        LOG.infof("Creating story: %s", story);
        story.persist();
        return story;
    }

    @GET
    @Path("/{id}")
    public Story getById(@PathParam("id") String id) {
        return Story.findById(new ObjectId(id));
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") String id) {
        Story story = Story.findById(new ObjectId(id));
        if (story != null) story.delete();
    }

    @PUT
    @Path("/{id}")
    public Story update(@PathParam("id") String id, Story updatedStory) {
        LOG.infof("Updating story with id %s: %s", id, updatedStory);
        Story story = Story.findById(new ObjectId(id));
        if (story != null) {
            story.title = updatedStory.title;
            story.content = updatedStory.content;
            story.images = updatedStory.images;
            story.audio = updatedStory.audio;
            story.persistOrUpdate();
        }
        return story;
    }
}