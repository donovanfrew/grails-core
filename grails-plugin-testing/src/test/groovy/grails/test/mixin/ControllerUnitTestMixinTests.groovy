package grails.test.mixin

import grails.converters.JSON
import grails.converters.XML
import grails.test.mixin.web.ControllerUnitTestMixin
import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import org.codehaus.groovy.grails.web.mime.MimeUtility
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

/**
 * Specification for the behavior of the ControllerUnitTestMixin
 *
 * @author Graeme Rocher
 *
 */
@TestMixin(ControllerUnitTestMixin)
class ControllerUnitTestMixinTests extends GroovyTestCase {


    void testRenderText() {
        def controller = mockController(TestController)

        controller.renderText()
        assert response.contentAsString == "good"
    }


    void testSimpleControllerRedirect() {

        def controller = mockController(TestController)

        controller.redirectToController()

        assert response.redirectedUrl == '/bar'
    }

    void testRenderView() {
        def controller = mockController(TestController)

        controller.renderView()

        assert "/test/foo" == controller.modelAndView.viewName
    }

    void testRenderXml() {
        def controller = mockController(TestController)

        controller.renderXml()

        assert "<book title='Great'/>" == controller.response.contentAsString
        assert "Great" == controller.response.xml.@title.text()
    }

    void testRenderJson() {

        def controller = mockController(TestController)

        controller.renderJson()

        assert '{"book":"Great"}' == controller.response.contentAsString
        assert "Great" == controller.response.json.book

    }

    void testRenderAsJson() {

        def controller = mockController(TestController)

        controller.renderAsJson()

        assert '{"foo":"bar"}' == controller.response.contentAsString
        assert "bar" == controller.response.json.foo

    }

    void testRenderState() {
        params.foo = "bar"
        request.bar = "foo"
        def controller = mockController(TestController)

        controller.renderState()

        def xml = response.xml

        assert xml.parameter.find { it.@name == 'foo' }.@value.text() == 'bar'
        assert xml.attribute.find { it.@name == 'bar' }.@value.text() == 'foo'
    }

    void testInjectedProperties() {
        assert request != null
        assert response != null
        assert servletContext != null
        assert params != null
        assert grailsApplication != null
        assert applicationContext != null
        assert webRequest != null
    }

    void testControllerAutowiring() {
        messageSource.addMessage("foo.bar", request.locale, "Hello World")

        def controller = mockController(TestController)

        controller.renderMessage()

        assert 'Hello World' == controller.response.contentAsString
    }

    void testRenderWithFormatXml() {
        def controller = mockController(TestController)

        request.format = 'xml'
        controller.renderWithFormat()

        assert '<?xml version="1.0" encoding="UTF-8"?><map><entry key="foo">bar</entry></map>' == response.contentAsString
    }

    void testRenderWithFormatHtml() {
        def controller = mockController(TestController)

        request.format = 'html'
        def model = controller.renderWithFormat()

        assert model?.foo == 'bar'
    }
}

class TestController {
    def renderText = {
        render "good"
    }

    def redirectToController = {
        redirect(controller:"bar")
    }

    def renderView = {
        render(view:"foo")
    }

    def renderXml = {
        render(contentType:"text/xml") {
            book(title:"Great")
        }
    }

    def renderJson = {
        render(contentType:"text/json") {
            book = "Great"
        }
    }

    def renderAsJson = {
        render( [foo:"bar"] as JSON )
    }

    def renderWithFormat = {
        def data = [foo:"bar"]
        withFormat {
            xml { render data as XML }
            html data
        }
    }

    def renderState = {
        render(contentType:"text/xml") {
            println params.foo
            println request.bar
            requestInfo {
                for(p in params) {
                    parameter(name:p.key, value:p.value)
                }
                request.each {
                    attribute(name:it.key, value:it.value)
                }
            }

        }
    }

    MessageSource messageSource
    @Autowired
    MimeUtility mimeUtility

    @Autowired
    LinkGenerator linkGenerator

    def renderMessage() {
        assert mimeUtility !=null
        assert linkGenerator != null
        render messageSource.getMessage("foo.bar", null, request.locale)
    }
}