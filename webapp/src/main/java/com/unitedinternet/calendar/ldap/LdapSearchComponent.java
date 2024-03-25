package com.unitedinternet.calendar.ldap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.List;

@Component
public class LdapSearchComponent {

    @Value("${ldap.search.filter}")
    private String ldapFilter;

    @Value("${ldap.search.base}")
    private String ldapBase;

    @Value("${ldap.search.attribute}")
    private String ldapAttribute;


    private final LdapTemplate ldapTemplate;

    public LdapSearchComponent(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public List<String> search() {
        // LDAP filter
        String filter = ldapFilter;

        List<String> results = ldapTemplate.search(
                ldapBase,
                filter, new AttributesMapper<String>() {
                    @Override
                    public String mapFromAttributes(Attributes attributes) throws NamingException {

                        return (String) attributes.get(ldapAttribute).get();
                    }
                });

        return results;
    }


}
