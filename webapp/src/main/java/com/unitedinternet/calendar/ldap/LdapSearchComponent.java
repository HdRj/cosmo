package com.unitedinternet.calendar.ldap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import java.util.List;

@Component
public class LdapSearchComponent {

    private final String ldapFilter;
    private final String ldapBase;
    private final String ldapAttribute;
    private final String searchScope;
    private final String countLimit;

    private final LdapTemplate ldapTemplate;

    public LdapSearchComponent(
            @Value("${ldap.email.filter}") String ldapFilter,
            @Value("${ldap.email.base}") String ldapBase,
            @Value("${ldap.email.attribute}") String ldapAttribute,
            @Value("${ldap.email.search-scope}") String searchScope,
            @Value("{ldap.email.count-limit}") String countLimit,
            LdapTemplate ldapTemplate
    ) {
        this.ldapFilter = ldapFilter;
        this.ldapBase = ldapBase;
        this.ldapAttribute = ldapAttribute;
        this.searchScope = searchScope;
        this.countLimit = countLimit;
        this.ldapTemplate = ldapTemplate;
    }

    public List<String> search(String uid, String oValue) {
        // LDAP filter
        String filter = ldapFilter.replace("%u",uid).replace("%o",oValue);

        SearchControls controls = new SearchControls();

        controls.setCountLimit(Long.parseLong(countLimit));
        controls.setReturningAttributes(new String[] {ldapAttribute});

        if(searchScope=="ONE"){
            controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        }else {
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        }

        List<String> results = ldapTemplate.search(
                ldapBase,
                filter,
                controls,
                new AttributesMapper<String>() {
                    @Override
                    public String mapFromAttributes(Attributes attributes) throws NamingException {

                        return (String) attributes.get(ldapAttribute).get();
                    }
                });

        return results;
    }

    public String getOrganization(String userDn) {
        List<String> organizationList = ldapTemplate.search(
                userDn,
                "(objectclass=*)",
                (AttributesMapper<String>) attrs -> {
                    try {
                        return (String) attrs.get("o").get();
                    } catch (NamingException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
        );
        return organizationList.isEmpty() ? null : organizationList.get(0);
    }


}
