/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// @flow
import React from 'react';
import { shallow, mount } from 'enzyme';
import Onboarding from '../Onboarding';
import { click, doAsync } from '../../../../helpers/testUtils';

jest.mock('../../../../api/users', () => ({
  skipOnboarding: () => Promise.resolve()
}));

const currentUser = { login: 'admin', isLoggedIn: true };

it('guides for on-premise', () => {
  const wrapper = shallow(
    <Onboarding
      currentUser={currentUser}
      onSkip={jest.fn()}
      organizationsEnabled={false}
      sonarCloud={false}
    />
  );
  expect(wrapper).toMatchSnapshot();

  // $FlowFixMe
  wrapper.instance().handleTokenDone('abcd1234');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('guides for sonarcloud', () => {
  const wrapper = shallow(
    <Onboarding
      currentUser={currentUser}
      onSkip={jest.fn()}
      organizationsEnabled={true}
      sonarCloud={true}
    />
  );
  expect(wrapper).toMatchSnapshot();

  // $FlowFixMe
  wrapper.instance().handleOrganizationDone('my-org');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  // $FlowFixMe
  wrapper.instance().handleTokenDone('abcd1234');
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('skips', () => {
  const onSkip = jest.fn();
  const wrapper = mount(
    <Onboarding
      currentUser={currentUser}
      onSkip={onSkip}
      organizationsEnabled={false}
      sonarCloud={false}
    />
  );
  click(wrapper.find('.js-skip'));
  return doAsync(() => {
    expect(onSkip).toBeCalled();
  });
});
