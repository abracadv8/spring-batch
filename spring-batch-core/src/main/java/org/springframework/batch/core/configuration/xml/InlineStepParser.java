/*
 * Copyright 2006-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.xml;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;step/&gt; elements inside a job. A step element
 * references a bean definition for a {@link org.springframework.batch.core.Step} and goes on to (optionally)
 * list a set of transitions from that step to others with &lt;next on="pattern"
 * to="stepName"/&gt;. Used by the {@link JobParser}.
 * 
 * @see JobParser
 * 
 * @author Dave Syer
 * @author Thomas Risberg
 * @since 2.0
 */
public class InlineStepParser extends AbstractStepParser {

	/**
	 * Parse the step and turn it into a list of transitions.
	 * 
	 * @param element the &lt;step/gt; element to parse
	 * @param parserContext the parser context for the bean factory
	 * @return a collection of bean definitions for {@link org.springframework.batch.core.job.flow.support.StateTransition}
	 * instances objects
	 */
	public Collection<RuntimeBeanReference> parse(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder stateBuilder = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.job.flow.support.state.StepState");
		String stepRef = element.getAttribute("name");
		String taskletRef = element.getAttribute("tasklet");

		if (!StringUtils.hasText(stepRef)) {
			parserContext.getReaderContext().error("The name attribute can't be empty for <" + element.getNodeName() + ">", element);
		}
		
		@SuppressWarnings("unchecked")
		List<Element> processTaskElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "tasklet");
		if (StringUtils.hasText(taskletRef)) {
			AbstractBeanDefinition bd = handleTaskletRef(element, taskletRef, parserContext);
			parserContext.registerBeanComponent(new BeanComponentDefinition(bd, stepRef));
			stateBuilder.addConstructorArgReference(stepRef);
		}
		else if (processTaskElements.size() > 0) {
			Element taskElement = processTaskElements.get(0);
			AbstractBeanDefinition bd = handleTaskletElement(element, taskElement, parserContext);
			parserContext.registerBeanComponent(new BeanComponentDefinition(bd, stepRef));
			stateBuilder.addConstructorArgReference(stepRef);
		}
		else if (StringUtils.hasText(stepRef)) {
				stateBuilder.addConstructorArgReference(stepRef);
		}
		else {
			parserContext.getReaderContext().error("Incomplete configuration detected while creating step with name " + stepRef, element);
		}
		return FlowParser.getNextElements(parserContext, stateBuilder.getBeanDefinition(), element);

	}

}
