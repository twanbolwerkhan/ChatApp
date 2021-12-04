/* eslint-disable @typescript-eslint/no-unsafe-member-access */
/* eslint-disable @typescript-eslint/no-unsafe-assignment */
import React from 'react';
import { createFeature, fassetValidations } from 'feature-u';
import SoundOverview from './component/SoundOverview';
import { link, route } from './route';
import featureName from './featureName';

export default createFeature({
  name: 'sounds',
  enabled: true,

  // our public face ...
  fassets: {
    define: {
      [`${featureName}.link.component`]: link,
    },

    defineUse: {
      [`${featureName}.route.component`]: route,
    },

    use: [['pagination.*', { required: true, type: fassetValidations.comp }]],
  },
  // inject our baseUI components into the root of our app
  appWillStart({ fassets, curRootAppElm }) {
    return <SoundOverview> {curRootAppElm} </SoundOverview>;
  },
});
