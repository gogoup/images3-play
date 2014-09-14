

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.images3.common.DuplicateTemplateNameException;
import com.images3.common.DuplicatedImagePlantNameException;
import com.images3.ImageS3;
import com.images3.common.NoSuchEntityFoundException;
import com.images3.common.UnremovableTemplateException;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Http;
import play.mvc.Results;

public class Global extends GlobalSettings {

    private Injector injector;
    
    public void onStart(Application app) {
        final ImageS3Provider imageS3Provider = new ImageS3Provider(
                app.configuration().getString("images3.conf"),
                app.configuration().getString("imageprocessor.conf"),
                app.configuration().getString("mongodb.conf")
                );
        injector = Guice.createInjector(new AbstractModule() {

            @Override
            protected void configure() {
                bind(ImageS3.class).toProvider(imageS3Provider).asEagerSingleton();
                bind(ObjectMapper.class).toProvider(new ObjectMapperProvider()).asEagerSingleton();;
            }
            
        });
        Logger.info("Application has started");
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }
    
    public <T> T getControllerInstance(Class<T> clazz) throws Exception {
        return injector.getInstance(clazz);
    }
    
    public Promise<Result> onError(Http.RequestHeader request, Throwable t) {
        if (DuplicatedImagePlantNameException.class.isInstance(t.getCause())) {
            DuplicatedImagePlantNameException exception =
                    (DuplicatedImagePlantNameException) t.getCause();
            String message = "ImagePlant name, \'" + exception.getName() + "\' has been taken.";
            return Promise.<Result>pure(Results.badRequest(message));
        }
        if (DuplicateTemplateNameException.class.isInstance(t.getCause())) {
            DuplicateTemplateNameException exception = (DuplicateTemplateNameException) t.getCause();
            String message = "Template name, \'" + exception.getName() + "\' has been taken.";
            return Promise.<Result>pure(Results.badRequest(message));
        }
        if (NoSuchEntityFoundException.class.isInstance(t.getCause())) {
            NoSuchEntityFoundException exception =
                    (NoSuchEntityFoundException) t.getCause();
            String message = "No such " + exception.getName() + " {" + exception.getId() + "} found.";
            return Promise.<Result>pure(Results.notFound(message));
        }
        if (UnremovableTemplateException.class.isInstance(t.getCause())) {
            UnremovableTemplateException exception =
                    (UnremovableTemplateException) t.getCause();
            String message = "Remove template " + "{" + exception.getId().getTemplateName() + "} is not allowed.";
            return Promise.<Result>pure(Results.notFound(message));
        }
        if (RuntimeException.class.isInstance(t.getCause())) {
            return Promise.<Result>pure(Results.badRequest(t.getCause().getMessage())); 
        }
        return Promise.<Result>pure(Results.internalServerError());
    }
    
    public Promise<Result> onHandlerNotFound(Http.RequestHeader request) {
        return Promise.<Result>pure(Results.redirect("/404.html"));
    }
    
}
