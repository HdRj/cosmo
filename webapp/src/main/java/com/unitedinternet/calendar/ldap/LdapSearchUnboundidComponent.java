package com.unitedinternet.calendar.ldap;

import com.unboundid.ldap.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

@Component
public class LdapSearchUnboundidComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapSearchUnboundidComponent.class);

    private final String ldapEmailFilter;
    private final String ldapAuthFilter;
    private final String ldapEmailBase;
    private final String ldapAuthBase;
    private final String ldapEmailAttribute;
    private final String searchScopeEmail;
    private final String countLimit;
    private final String ldapAuthAttribute;
    private final String searchScopeAuth;

    public LdapSearchUnboundidComponent(
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

    // get the values of the attributes that we need to search for the
    // email address
    public HashMap<String, String> getVariables(String userDn, LDAPConnection connection) {

        String[] searchAttributes = ldapAuthAttribute.split(",");
        HashMap<String, String> AttributeValues = new HashMap<>(); 

        try {
            SearchScope scope = SearchScope.BASE;
            SearchRequest searchRequest = new SearchRequest(userDn, scope,"(objectclass=*)", searchAttributes);
            SearchResult searchResult = connection.search(searchRequest);

            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                for (String attribute: searchAttributes) {
                    AttributeValues.put(attribute, entry.getAttributeValue(attribute));
                }
            }
        } catch (LDAPException e) {
            LOGGER.error("Get organization exception {}",e.getMessage());
        }

        return AttributeValues;
    }

    //replace filter variables with their found values
    public String substituteVariables(HashMap<String, String> attributes, String filter) {

        for ( HashMap.Entry<String, String> attribute : attributes.entrySet() ) {
            filter = filter.replaceAll("%" + attribute.getKey() + "\\b", attribute.getValue());
        }

        return filter;
    }

    // search for the email address of a user
    public List<String> search(String uid, HashMap<String, String> criteria, LDAPConnection connection) {

        String filter = substituteVariables(criteria,ldapEmailFilter);

        String[] attributesToReturn = new String[]{ldapEmailAttribute};
        int countLimitValue = Integer.parseInt(countLimit);
        SearchScope scope = stringToSearchScope(searchScopeEmail);

        List<String> results = new ArrayList<>();

        try {
            SearchRequest searchRequest = new SearchRequest(ldapEmailBase, scope, filter, attributesToReturn);
            searchRequest.setSizeLimit(countLimitValue);
            SearchResult searchResult = connection.search(searchRequest);

            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                results.add(entry.getAttributeValue(ldapEmailAttribute));
            }
        } catch (LDAPException e) {
            LOGGER.error("Search exception: " + e.getMessage());
        }

        return results;
    }

    // search for user with manager
    public List<String> searchUser(String uid,  LDAPConnection connection, String attribute) {

        String filter = ldapAuthFilter.replace("%u", uid);
        String[] attributesToReturn = new String[]{attribute};
        int countLimitValue = Integer.parseInt(countLimit);
        SearchScope scope = stringToSearchScope(searchScopeAuth);

        List<String> results = new ArrayList<>();

        try {
            SearchRequest searchRequest = new SearchRequest(ldapAuthBase, scope, filter, attributesToReturn);
            searchRequest.setSizeLimit(countLimitValue);
            SearchResult searchResult = connection.search(searchRequest);

            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                results.add(entry.getDN());
            }
        } catch (LDAPException e) {
            LOGGER.error("Search user exception: " + e.getMessage());
        }

        return results;
    }

    private SearchScope stringToSearchScope(String value){
        SearchScope searchScope;
        switch (value){
            case "BASE":
                searchScope = SearchScope.BASE;
                break;
            case "ONE":
                searchScope = SearchScope.ONE;
                break;
            case  "SUBTREE":
                searchScope = SearchScope.SUBORDINATE_SUBTREE;
                break;
            case "SUB":
                searchScope = SearchScope.SUB;
                break;
            default:
                searchScope = SearchScope.SUBORDINATE_SUBTREE;
        }
        return searchScope;
    }
}
