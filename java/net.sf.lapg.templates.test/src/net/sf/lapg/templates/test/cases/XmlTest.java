package net.sf.lapg.templates.test.cases;

import junit.framework.Assert;
import net.sf.lapg.templates.api.impl.ClassTemplateLoader;
import net.sf.lapg.templates.model.xml.XmlModel;
import net.sf.lapg.templates.model.xml.XmlNavigationFactory;
import net.sf.lapg.templates.model.xml.XmlNode;
import net.sf.lapg.templates.test.TemplateTestCase;

public class XmlTest extends TemplateTestCase{

	public void testSelector() {
		XmlNode n = XmlModel.load(" <r><user name='jone'/>go<user name='go'/></r> ");

		TestEnvironment env = new TestEnvironment(new XmlNavigationFactory(), new ClassTemplateLoader(getClass().getClassLoader(), "net/sf/lapg/templates/test/ltp"));

		// test 1
		String q = env.executeTemplate("loop.xmldo", n, null);
		Assert.assertEquals("jone\r\ngo\r\n", q);
		env.assertEmptyErrors();

		q = env.executeTemplate("loop.selectit", n, null);
		Assert.assertEquals("name=\"jone\"\r\n", q);
		env.assertEmptyErrors();
	}
}
