
package com.android.sdklib.repositoryv2.sources.generated.v3;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import com.android.repository.impl.sources.generated.v1.SiteType;


/**
 * DO NOT EDIT
 * This file was generated by xjc from sdk-sites-list-3.xsd. Any changes will be lost upon recompilation of the schema.
 * See the schema file for instructions on running xjc.
 * 
 * 
 *                 Trivial siteType extension specifying that this is a addon site
 *             
 * 
 * <p>Java class for addonSiteType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="addonSiteType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://schemas.android.com/repository/android/sites-common/1}siteType"&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "addonSiteType")
@SuppressWarnings({
    "override",
    "unchecked"
})
public class AddonSiteType
    extends SiteType
    implements com.android.sdklib.repositoryv2.sources.RemoteSiteType.AddonSiteType
{


    public ObjectFactory createFactory() {
        return new ObjectFactory();
    }

}
