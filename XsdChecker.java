import com.sun.xml.xsom.*;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.visitor.XSWildcardVisitor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Reader;
import java.util.*;


public class XsdChecker {
	List<String> errorMsg = Collections.synchronizedList(new ArrayList<String>());
	private static final Log log = LogFactory.getLog(XsdChecker.class);
	
    public HashMap<String, String> getXsdTypeMap() {
        return xsdTypeMap;
    }

    // the flattened type map from XSD
    private HashMap<String, String> xsdTypeMap = new HashMap<String, String>();

    public HashSet<String> getArrayElementSet() {
        return arrayElementSet;
    }

    // a set that holds all the array types
    private HashSet<String> arrayElementSet = new HashSet<String>();

	public List<String>  getSchemaInvalidErrors(Reader reader) throws Exception {
		//clear the errors
		errorMsg.clear();
		XSOMParser parser = new XSOMParser();

        parser.parse(reader);

        XSSchemaSet sset = parser.getResult();
        XSSchema schema = sset.getSchema(1);
        log.debug("Schema being registered="+schema.toString());

        Iterator<XSElementDecl> itrElements = schema.iterateElementDecls();

        while(itrElements.hasNext()) {

            XSElementDecl xsElementDecl = (XSElementDecl) itrElements.next();
            XSType xsType = xsElementDecl.getType();

            if (xsType.isSimpleType()) {
                log.debug("Element has simple type restriction="+xsType.asSimpleType().isRestriction());
//                System.out.println(xsElementDecl.getName() + ":" + xsType.getName());
                xsdTypeMap.put(xsElementDecl.getName(), xsType.getName());
            }
            else {
                XSComplexType xscomp = xsElementDecl.getType().asComplexType();
                if (xscomp != null) {
                    XSContentType xscont = xscomp.getContentType();
                    XSParticle particle = xscont.asParticle();
//                    System.out.println(xsElementDecl.getName() + ":" + xscomp.getName());
                    xsdTypeMap.put(xsElementDecl.getName(), xscomp.getName());
                    getElementsRecursively(particle, xsElementDecl.getName() + ".");
                }
            }

        }
        return errorMsg;

    }

    private static void initRestrictions(XSSimpleType xsSimpleType)
    {
        XSRestrictionSimpleType restriction = xsSimpleType.asRestriction();
        if (restriction != null)
        {
            Vector<String> enumeration = new Vector<String>();
            Vector<String> pattern     = new Vector<String>();

            for (XSFacet facet : restriction.getDeclaredFacets())
            {
            	log.debug("facet.getName()="+facet.getName());
            	log.debug("facet.getValue().value="+facet.getValue().value);
              
            }

        }
    }

    /*
     * recursive helper method of getXmlElements
     * note that since we don't know the "deepness" of the
     * schema a recursive way of implementation was necessary
     */
    private void getElementsRecursively(XSParticle xsp, String currentElementPath) {
        XSWildcardVisitor wildcardVisitor  = new XSWildcardVisitor() {
            @Override
            public void any(XSWildcard.Any wc) {
                errorMsg.add("xs:any not allowed"+wc.toString());
                log.debug("xs:any not allowed"+wc.toString());
			}

            @Override
            public void other(XSWildcard.Other wc) {
                errorMsg.add("xs:other not allowed"+wc.toString());
                log.debug("xs:other not allowed");

            }

            @Override
            public void union(XSWildcard.Union wc) {
                errorMsg.add("xs:union not allowed"+wc.toString());
                log.debug("xs:union not allowed");
            }
        };

        if(xsp != null){
            XSTerm term = xsp.getTerm();
            XSElementDecl xsElementDecl = term.asElementDecl();

            if(term.isElementDecl()) {
                String typeName = xsElementDecl.getType().getName();
                String elementName = xsElementDecl.getName();
//                System.out.println(elementName + ":" + typeName);

                // check if the element is an array
                if (xsp.getMaxOccurs().intValue() == -1) {
                    arrayElementSet.add(currentElementPath + elementName);
                }

                    if (typeName != null)
                        xsdTypeMap.put(currentElementPath + elementName, typeName);
                    currentElementPath = currentElementPath + elementName + ".";

//                System.out.println("current path is " + currentElementPath);

                XSComplexType xscmp =  xsElementDecl.getType().asComplexType();
                //---
                if (xscmp == null){
                    if(xsp.getMinOccurs().intValue() != 1) {
                        log.debug("min occurs="+xsp.getMinOccurs());
                        log.debug("Element in error=" + elementName);
                        errorMsg.add("min occurs not allowed for "+ elementName);
                    }
                    //initRestrictions(term.asElementDecl().getType().asSimpleType());
	              /*   if (term.asElementDecl().getType().asSimpleType().asRestriction() != null) {
	                	 log.debug("Element in error (no restrictions)="+term.asElementDecl().getName());
	                 }*/

                } else {
                    XSContentType xscont = xscmp.getContentType();
                    XSParticle particle = xscont.asParticle();
                    if (xscmp.getAttGroups().size() > 0) {
                        log.debug("Error!! no attribute grps for " + elementName);
                        errorMsg.add("Error!! no attribute grps for " + elementName);
                    }
                    if (xscmp.getAttributeUses().size() > 0) {
                        errorMsg.add("Error!! no attributes for " + elementName);
                        log.debug("Error!! no attributes for "+ elementName);

                    }
                    getElementsRecursively(particle, currentElementPath);
                    if (xscmp.getContentType().asSimpleType() != null) { // simple content
                        log.debug("CT with simpleContent, CT name: " + xscmp.getContentType().asSimpleType());
                        if (xscmp.getContentType().asSimpleType().getBaseType() !=null) {
                            log.debug("Error! No extensions");
                            errorMsg.add("Error!! No extensions ");
                        }
                    }
                    if (xscmp.isMixed()) {
                        log.debug("Error!! no mixed content type for " + elementName);
                        errorMsg.add("Error!! no mixed content type for " + elementName);
                    }
                }
                //---
            } else if(term.isModelGroup()){
                XSModelGroup model = term.asModelGroup();
                XSParticle[] parr = model.getChildren();
                for(XSParticle partemp : parr ){
                    getElementsRecursively(partemp, currentElementPath);
                }
            } else if (term.isWildcard()) {
                term.asWildcard().visit(wildcardVisitor);
            }
        }
    }

    // The code is here just in case we need to validate the xml against the xsd outside the conversion. (The conversion
    // actually serves as some sort of validation itself.)
//    public static boolean validateXMLSchema(String xsdPath, String xmlPath) {
//
//        try {
//            SchemaFactory factory =
//                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
//            Schema schema = factory.newSchema(new File(xsdPath));
//            Validator validator = schema.newValidator();
//            validator.validate(new StreamSource(new File(xmlPath)));
//        } catch (IOException e) {
//            System.out.println("Exception: "+e.getMessage());
//            return false;
//        } catch (SAXException e) {
//            System.out.println("Exception: "+e.getMessage());
//            return false;
//        }
//        return true;
//    }
}
