/*******************************************************************************
 * Copyright (c) 2014 Alex Panchenko
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alex Panchenko - initial API and Implementation
 *******************************************************************************/
package org.eclipse.dltk.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.osgi.framework.Bundle;

public class BundleUtil {

    public static Bundle getBundle(IConfigurationElement configElement) {
        final IContributor contributor = configElement.getContributor();
        if (contributor instanceof RegistryContributor) {
            final Bundle bundle = Platform.getBundle(((RegistryContributor) contributor).getActualName());
            if (bundle != null) {
                return bundle;
            }
        }
        return Platform.getBundle(contributor.getName());
    }

    public static File getFile(Bundle bundle, String path) throws IOException {
        final URL url = FileLocator.toFileURL(bundle.getEntry(path));
        try {
            // encode spaces - see https://bugs.eclipse.org/bugs/show_bug.cgi?id=145096
            return toFile(url);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    public static File toFile(final URL url) throws URISyntaxException {
        final URI u = new URI(url.toString().replace(" ", "%20"));
        return new File(u);
    }
}
