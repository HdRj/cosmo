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

    private final String ldapFilter;
    private final String ldapBase;
    private final String ldapEmailAttribute;
    private final String searchScope;
    private final String countLimit;
    private final String ldapAuthAttribute;

    public LdapSearchComponent(
            @Value("${ldap.email.filter}") String ldapFilter,
            @Value("${ldap.email.base}") String ldapBase,
            @Value("${ldap.email.attribute}") String ldapEmailAttribute,
            @Value("${ldap.email.search-scope}") String searchScope,
            @Value("${ldap.email.count-limit}") String countLimit,
            @Value("${ldap.auth.attribute}") String ldapAuthAttribute
    ) {
        this.ldapFilter = ldapFilter;
        this.ldapBase = ldapBase;
        this.ldapEmailAttribute = ldapEmailAttribute;
        this.searchScope = searchScope;
        this.countLimit = countLimit;
        this.ldapAuthAttribute = ldapAuthAttribute;
    }

    public String getOrganization(String userDn, DirContext dirContext) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{ldapAuthAttribute.split(",")[1]});

        try {
            NamingEnumeration<SearchResult> results = dirContext.search(userDn, "(objectclass=*)", controls);

            while (results.hasMore()) {
                SearchResult searchResult = results.next();
                Attributes attrs = searchResult.getAttributes();
                return (String) attrs.get("o").get();
            }
        } catch (NamingException e) {
            LOGGER.error("Get organization exception {}",e.getMessage());
        }

        return null;
    }

    public List<String> search(String uid, String oValue, DirContext dirContext) {

        String filter = ldapFilter.replace("%u", uid).replace("%o", oValue);

        SearchControls controls = new SearchControls();
        controls.setCountLimit(Long.parseLong(countLimit));
        controls.setReturningAttributes(new String[]{ldapEmailAttribute});
        controls.setSearchScope(searchScope.equals("ONE") ? SearchControls.ONELEVEL_SCOPE : SearchControls.SUBTREE_SCOPE);

        List<String> results = new ArrayList<>();

        try {
            NamingEnumeration<SearchResult> searchResults = dirContext.search(ldapBase, filter, controls);

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
}
