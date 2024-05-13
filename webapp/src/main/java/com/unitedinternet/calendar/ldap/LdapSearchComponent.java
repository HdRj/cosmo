package com.unitedinternet.calendar.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.ArrayList;
import java.util.List;

@Component
public class LdapSearchComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapSearchComponent.class);

    private final String ldapEmailFilter;
    private final String ldapAuthFilter;
    private final String ldapEmailBase;
    private final String ldapAuthBase;
    private final String ldapEmailAttribute;
    private final String searchScopeEmail;
    private final String countLimit;
    private final String ldapAuthAttribute;
    private final String searchScopeAuth;

    public LdapSearchComponent(
            @Value("${ldap.email.filter}") String ldapEmailFilter,
            @Value("${ldap.auth.filter}") String ldapAuthFilter,
            @Value("${ldap.email.base}") String ldapEmailBase,
            @Value("${ldap.auth.base}") String ldapAuthBase,
            @Value("${ldap.email.attribute}") String ldapEmailAttribute,
            @Value("${ldap.email.search-scope}") String searchScopeEmail,
            @Value("${ldap.email.count-limit}") String countLimit,
            @Value("${ldap.auth.attribute}") String ldapAuthAttribute,
            @Value("${ldap.auth.search-scope}") String searchScopeAuth
    ) {
        this.ldapEmailFilter = ldapEmailFilter;
        this.ldapAuthFilter = ldapAuthFilter;
        this.ldapEmailBase = ldapEmailBase;
        this.ldapAuthBase = ldapAuthBase;
        this.ldapEmailAttribute = ldapEmailAttribute;
        this.searchScopeEmail = searchScopeEmail;
        this.countLimit = countLimit;
        this.ldapAuthAttribute = ldapAuthAttribute;
        this.searchScopeAuth = searchScopeAuth;
    }

    public String getOrganization(String userDn, DirContext dirContext) {
        String searchAttribute = ldapAuthAttribute.split(",")[1];
        SearchControls controls = new SearchControls();
        controls.setSearchScope(stringToSearchControl(searchScopeAuth));
        controls.setReturningAttributes(new String[]{searchAttribute});

        try {
            NamingEnumeration<SearchResult> results = dirContext.search(userDn, "(objectclass=*)", controls);

            while (results.hasMore()) {
                SearchResult searchResult = results.next();
                Attributes attrs = searchResult.getAttributes();
                return (String) attrs.get(searchAttribute).get();
            }
        } catch (NamingException e) {
            LOGGER.error("Get organization exception {}",e.getMessage());
        }

        return null;
    }

    public List<String> search(String uid, String oValue, DirContext dirContext) {

        String filter = ldapEmailFilter.replace("%u", uid).replace("%o", oValue);

        SearchControls controls = new SearchControls();
        controls.setCountLimit(Long.parseLong(countLimit));
        controls.setReturningAttributes(new String[]{ldapEmailAttribute});
        controls.setSearchScope(stringToSearchControl(searchScopeEmail));

        List<String> results = new ArrayList<>();

        try {
            NamingEnumeration<SearchResult> searchResults = dirContext.search(ldapEmailBase, filter, controls);

            while (searchResults.hasMore()) {
                SearchResult searchResult = searchResults.next();
                Attributes attributes = searchResult.getAttributes();
                results.add((String) attributes.get(ldapEmailAttribute).get());
            }
        } catch (NamingException e) {
            LOGGER.error("Search exception: {}", e.getMessage());
        }

        return results;
    }

    public List<String> searchUser(String uid, DirContext dirContext, String attribute) {

        String filter = ldapAuthFilter.replace("%u",uid);

        SearchControls controls = new SearchControls();
        controls.setCountLimit(Long.parseLong(countLimit));
        controls.setReturningAttributes(new String[]{attribute});
        controls.setSearchScope(stringToSearchControl(searchScopeAuth));

        List<String> results = new ArrayList<>();

        try {
            NamingEnumeration<SearchResult> searchResults = dirContext.search(ldapAuthBase, filter, controls);

            while (searchResults.hasMore()) {
                SearchResult searchResult = searchResults.next();
                results.add(searchResult.getNameInNamespace());
            }
        } catch (NamingException e) {
            LOGGER.error("Search user exception: {}", e.getMessage());
        }

        return results;
    }

    private int stringToSearchControl(String value){
        int searchControl;
        switch (value){
            case "BASE":
                searchControl = SearchControls.OBJECT_SCOPE;
                break;
            case "ONE":
                searchControl = SearchControls.ONELEVEL_SCOPE;
                break;
            case  "SUBTREE":
                searchControl = SearchControls.SUBTREE_SCOPE;
                break;
            default:
                searchControl = SearchControls.SUBTREE_SCOPE;
        }
        return searchControl;
    }

}
