package com.fabelfabrik.resource;

import com.fabelfabrik.model.Story;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

@Path("/api/stories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StoryResource {

    private static final Logger LOG = Logger.getLogger(StoryResource.class);

    @GET
    public List<Story> getAll() {
        List<Story> stories = Story.listAll();
        // Sort stories by createdAt in descending order (newest first)
        stories.sort(Comparator.comparing(story -> story.createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        LOG.infof("Found %d stories", stories.size());
        return stories;
    }

    @POST
    public Story create(Story story) {
        LOG.infof("Creating story: %s", story);
        // Set createdAt if not already set
        if (story.createdAt == null) {
            story.createdAt = Instant.now();
        }
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
            story.audioUrl = updatedStory.audioUrl;
            story.videoUrl = updatedStory.videoUrl;
            story.persistOrUpdate();
        }
        return story;
    }

    @PUT
    @Path("/{id}/images")
    public Story addImages(@PathParam("id") String id, List<String> imagePaths) {
        LOG.infof("Adding images to story with id %s: %s", id, imagePaths);
        Story story = Story.findById(new ObjectId(id));
        if (story != null) {
            if (story.images == null) {
                story.images = new ArrayList<>();
            }
            story.images.addAll(imagePaths);
            story.persistOrUpdate();
        }
        return story;
    }

    @DELETE
    @Path("/{id}/images/{imagePath: .+}")
    public Story removeImage(@PathParam("id") String id, @PathParam("imagePath") String imagePath) {
        LOG.infof("Removing image %s from story with id %s", imagePath, id);
        Story story = Story.findById(new ObjectId(id));
        if (story != null && story.images != null) {
            story.images.remove(imagePath);
            story.persistOrUpdate();
        }
        return story;
    }

    @POST
    @Path("/{id}/ratings")
    public Response addRating(@PathParam("id") String id, RatingRequest ratingRequest) {
        LOG.infof("Adding rating %d to story with id %s", ratingRequest.rating, id);

        // Validate rating value (must be between 1 and 5)
        if (ratingRequest.rating < 1 || ratingRequest.rating > 5) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Rating must be between 1 and 5")
                    .build();
        }

        Story story = Story.findById(new ObjectId(id));
        if (story == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Story not found")
                    .build();
        }
        story.addRating(ratingRequest);
        story.persistOrUpdate();
        return Response.ok(story).build();
    }

}
