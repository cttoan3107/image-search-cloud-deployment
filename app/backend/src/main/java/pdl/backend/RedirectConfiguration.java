/*
  Global role of this class:

  - Configures route forwarding for the frontend (SPA)
  - Redirects all non-static routes to index.html
  - Ensures Vue Router works correctly with direct URL access
*/

package pdl.backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RedirectConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward all routes that do NOT contain a dot (exclude static resources)
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
        registry.addViewController("/**/{path:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}