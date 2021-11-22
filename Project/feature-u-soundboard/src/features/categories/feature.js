import React from 'react';
import { createFeature, fassetValidations } from 'feature-u';
import CategoryView from './component/CategoryView';

export default createFeature({
  name: 'categories',

  // our public face ...
  fassets: {
    define: {},

    defineUse: {},

    use: [['sounds.*', { required: true, type: fassetValidations.comp }]],
  },
  // inject our baseUI components into the root of our app
  appWillStart({ fassets, curRootAppElm }) {
    console.log(fassets.sounds.SoundOverview);
    return <CategoryView Child={fassets.sounds.SoundOverview}> </CategoryView>;
  },
});
