/*
 * Copyright © 2010 Red Hat, Inc.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.redhat.rhevm.api.command.base;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.ws.rs.core.Response;

import org.apache.abdera.i18n.templates.Template;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import com.redhat.rhevm.api.model.Action;
import com.redhat.rhevm.api.model.BaseResource;
import com.redhat.rhevm.api.model.Fault;
import com.redhat.rhevm.api.model.LinkHeader;
import com.redhat.rhevm.api.model.Link;
import com.redhat.rhevm.api.model.Status;

/**
 * Client to make RESTful invocations via a direct API (to avoid leakage
 * of the URI structure, as would occur via a proxy-based API).
 */
public class BaseClient {

    protected static final String SEARCH_RELATION = "/search";
    protected static final String HTTP_SCHEME = "http";
    protected static final String HTTPS_SCHEME = "https";

    protected String baseUrl;
    protected String user;
    protected String secret;

    protected String trustStorePath;
    protected String trustStorePassword;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public <S> S get(String href, Class<S> clz) throws Exception {
        S ret = clz.newInstance();
        Response r = null;
        Exception failure = null;
        try {
            WebClient get = getClient(href);
            r = getClient(href).path("/").accept("application/xml").get();
        } catch (Exception e) {
           failure = e;
        }

        if (failure != null || r.getStatus() != 200) {
            String baseError = "cannot follow " + href + ", failed with ";
            diagnose(baseError, failure, r, 200);
        } else {
            ret = unmarshall(r, clz);
        }
        return ret;
    }

    public <S> S getRel(String rel, Class<S> clz) throws Exception {
        String top = getTopLink(rel);
        if (top != null) {
            return get(getBaseUri(top, null), clz);
        } else {
            return clz.newInstance();
        }
    }

    public <S> S getCollection(String rel, Class<S> clz) throws Exception {
        return getCollection(rel, clz, null);
    }

    public <S> S getCollection(String rel, Class<S> clz, String constraint) throws Exception {
        S ret = clz.newInstance();
        Response r = null;
        Exception failure = null;
        String top = constraint == null
                     ? getTopLink(rel)
                     : getTopLink(rel + SEARCH_RELATION);

        if (top != null) {
            try {
                WebClient get = getClient(getBaseUri(top, constraint));
                r = get.path("/").accept("application/xml").get();
            } catch (Exception e) {
                failure = e;
            }

            if (failure != null || r.getStatus() != 200) {
                String baseError = "cannot follow " + top + ", failed with ";
                diagnose(baseError, failure, r, 200);
            } else {
                ret = unmarshall(r, clz);
            }
        }
        return ret;
    }

    public <S> S getCollection(Link link, Class<S> clz) throws Exception {
        S ret = clz.newInstance();
        Response r = null;
        Exception failure = null;

        try {
            WebClient get = getClient(getBaseUri(link.getHref(), null));
            r = get.path("/").accept("application/xml").get();
        } catch (Exception e) {
            failure = e;
        }

        if (failure != null || r.getStatus() != 200) {
            String baseError = "cannot follow " + link.getHref() + ", failed with ";
            diagnose(baseError, failure, r, 200);
        } else {
            ret = unmarshall(r, clz);
        }

        return ret;
    }

    public void doAction(String verb, Action action, Link link, boolean detail) throws Exception {
        Response r = null;
        Exception failure = null;

        try {
            WebClient post = getClient(link.getHref());
            r = post.path("/").post(action);
        } catch (Exception e) {
            failure = e;
        }

        int expectedStatus = action.isAsync() ? 202 : 200;
        if (failure != null || r.getStatus() != expectedStatus) {
            diagnose(verb + " failed with ", failure, r, expectedStatus);
        } else {
            Action reaction = unmarshall(r, Action.class);
            String monitor =
                Status.COMPLETE.equals(reaction.getStatus())
                || Status.FAILED.equals(reaction.getStatus())
                ? ""
                : ", monitor @ " + reaction.getHref();
            System.out.println(verb + " " + reaction.getStatus() + monitor);
            if (Status.FAILED.equals(reaction.getStatus()) && reaction.isSetFault()) {
                System.out.println("[" + reaction.getFault().getReason() + "]");
                if (detail) {
                    System.out.println("[" + reaction.getFault().getDetail() + "]");
                }
            }
        }
    }

    public <T extends BaseResource> T doUpdate(T resource, Class<T> clz, String field, String href, String localName) throws Exception {
        Response r = null;
        Exception failure = null;
        T ret = null;

        try {
            WebClient put = getClient(href);
            r = put.path("/").put(new JAXBElement<T>(new QName("", localName), clz, null, resource));
            if (r.getStatus() == 200) {
                ret = unmarshall(r, clz);
            }
        } catch (Exception e) {
            failure = e;
        }

        if (failure != null || r.getStatus() != 200) {
            diagnose("update of " + field + " failed with ", failure, r, 200, 409);
        }
        return ret;
    }

    public <T extends BaseResource> T doAdd(T resource, Class<T> clz, String href, String localName) throws Exception {
        Response r = null;
        Exception failure = null;
        T ret = null;

        try {
            WebClient post = getClient(href);
            r = post.path("/").post(new JAXBElement<T>(new QName("", localName), clz, null, resource));
            ret = unmarshall(r, clz);
        } catch (Exception e) {
            failure = e;
        }

        if (failure != null || r.getStatus() != 201) {
            diagnose("addition of " + resource.getName() + " failed with ", failure, r, 201);
        }
        return ret;
    }

    public void doRemove(BaseResource resource) throws Exception {
        Response r = null;
        Exception failure = null;

        try {
            WebClient delete = getClient(resource.getHref());
            r = delete.path("/").delete();
        } catch (Exception e) {
            failure = e;
        }

        int expectedStatus = 204;
        if (failure != null || r.getStatus() != expectedStatus) {
            diagnose("remove failed with ", failure, r, expectedStatus);
        } else {
            System.out.println("remove succeeded");
        }
    }

    protected String getTopLink(String rel) {
        String ret = null;
        Response links = null;
        Exception failure = null;

        BindingFactoryManager manager =
            BusFactory.getDefaultBus().getExtension(BindingFactoryManager.class);
        manager.registerBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID,
                                       new JAXRSBindingFactory());

        try {
            WebClient head = getClient(getBaseUrl());
            links = head.path("/").head();
            ret = links.getStatus() == 200 ? getLink(links, rel) : null;
        } catch (Exception e) {
            failure = e;
        }

        if (failure != null || links.getStatus() != 200) {
            String baseError = "cannot follow " + getBaseUrl() + ", failed with ";
            diagnose(baseError, failure, links, 200);
        }

        return ret;
    }

    private WebClient getClient(String href) throws Exception {

        return configureTLS(user == null || secret == null
                            ? WebClient.create(absolute(href))
                            : WebClient.create(absolute(href), user, secret, null));
   }

    private WebClient configureTLS(WebClient client) throws Exception {
        if (client.getBaseURI().getScheme().startsWith(HTTPS_SCHEME)
            && !(trustStorePath == null || trustStorePassword ==null)) {

            HTTPConduit conduit =
                (HTTPConduit)WebClient.getConfig(client).getConduit();

            TLSClientParameters tlsParameters = new TLSClientParameters();
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(trustStorePath),
                            trustStorePassword.toCharArray());
            TrustManagerFactory trustFactory =
                TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustFactory.init(trustStore);
            tlsParameters.setTrustManagers(trustFactory.getTrustManagers());
            // allow a hostname mismatch
            tlsParameters.setDisableCNCheck(true);
            conduit.setTlsClientParameters(tlsParameters);
        }
        return client;
    }

    private <S> S unmarshall(Response r, Class<S> clz) throws Exception {
        InputStream is = (InputStream)r.getEntity();
        JAXBContext context = JAXBContext.newInstance(clz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<S> root = unmarshaller.unmarshal(new StreamSource(is), clz);
        return root.getValue();
    }

    private String getLink(Response r, String rel) {
        String ret = null;
        List<Object> links = r.getMetadata().get("Link");
        for (Object o : links) {
            for (String l : ((String)o).split(",")) {
                Link link = LinkHeader.parse(l);
                if (rel.equals((link.getRel()))) {
                    ret = absolute(link.getHref());
                }
            }
        }
        return ret;
    }

    public String getLink(List<Link> links, String rel) {
        String ret = null;
        for (Link link : links) {
            if (rel.equals((link.getRel()))) {
                ret = absolute(link.getHref());
            }
        }
        return ret;
    }

    public static String pad(String f, int width) {
        return pad(f, width, true);
    }

    public static String pad(String f, int width, boolean bracket) {
        StringBuffer field = new StringBuffer(bracket ? "[" : " ").append(f);
        for (int i = 0 ; i < width - value(f).length() ; i++) {
            field.append(" ");
        }
        return field.append(bracket ? "] " : "  ").toString();
    }

    public static String value(String f) {
        return f != null ? f : "";
    }

    private String getBaseUri(String href, String constraint) {
        String ret = absolute(href);
        if (constraint != null) {
            Template t = new Template(href);
            Map<String, String> m = new HashMap<String, String>();
            m.put("query", constraint);
            ret = t.expand(m);
        }
        return ret;
    }

    private String absolute(String href) {
        return href.startsWith(HTTP_SCHEME) || href.startsWith(HTTPS_SCHEME)
               ? href
               : href.startsWith("/")
                 ? getBaseUrl() + href
                 : getBaseUrl() + "/" + href;
    }

    private void diagnose(String baseError, Exception failure, Response r, int expected) {
        diagnose(baseError, failure, r, expected, -1);
    }

    private void diagnose(String baseError, Exception failure, Response r, int expected, int faultStatus) {
        if (failure != null) {
            System.err.println(baseError + failure.getClass().getSimpleName() + ":" + failure.getMessage());
        } else if (r.getStatus() != expected) {
            System.err.println(baseError + getStatus(r));
            if (r.getStatus() == faultStatus) {
                try {
                    Fault fault = unmarshall(r, Fault.class);
                    System.out.println("reason: " + fault.getReason());
                    System.out.println("detail: " + fault.getDetail());
                } catch (Exception e) {
                    // be tolerant of fault unmarshall failures
                }
            }
        }
    }

    private String getStatus(Response r) {
        Response.Status status = Response.Status.fromStatusCode(r.getStatus());
        return new StringBuffer(Integer.toString(r.getStatus()))
                       .append(" ").append(status.toString())
                       .append(" (").append(status.getFamily()).append(")")
                       .toString();
    }

}
