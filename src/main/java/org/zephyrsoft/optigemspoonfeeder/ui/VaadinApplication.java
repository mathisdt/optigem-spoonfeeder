package org.zephyrsoft.optigemspoonfeeder.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;

@Theme("custom-theme")
@Push(PushMode.DISABLED)
public class VaadinApplication implements AppShellConfigurator {

}
