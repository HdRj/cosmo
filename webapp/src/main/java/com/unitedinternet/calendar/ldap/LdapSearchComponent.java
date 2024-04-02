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

    private final String ldapFilterO;
    private final String ldapBase;
    private final String ldapAttribute;

    private final LdapTemplate ldapTemplate;

    public LdapSearchComponent(
            @Value("${ldap.email.filter.o}") String ldapFilterO,
            @Value("${ldap.email.base}") String ldapBase,
            @Value("${ldap.email.attribute}") String ldapAttribute,
            LdapTemplate ldapTemplate
    ) {
        this.ldapFilterO = ldapFilterO;
        this.ldapBase = ldapBase;
        this.ldapAttribute = ldapAttribute;
        this.ldapTemplate = ldapTemplate;
    }

    public List<String> search(String uid, String oValue) {
        // LDAP filter
        String filter = "(&(sendmailMTAMapValue="+uid+")(o="+oValue+"))";

        List<String> results = ldapTemplate.search(
                ldapBase,
                filter,
                new AttributesMapper<String>() {
                    @Override
                    public String mapFromAttributes(Attributes attributes) throws NamingException {

                        return (String) attributes.get(ldapAttribute).get();
                    }
                });

        return results;
    }

    public List<String> search(String uid) {
        return search(uid, ldapFilterO);
    }

}
