import React, { useEffect } from 'react';
import { Box, Text, Button } from 'grommet';
import { useAppContext } from '../AppContext';
import { UserTaskAppLayout } from './UserTaskAppLayout';
import i18n from '../i18n';
import { useTranslation } from 'react-i18next';

i18n.addResources('en', 'no-usertask-given', {
      "hint": "Unfortunately, the task cannot be shown at the moment!",
      "retry": "Retry...",
      "task-does-not-exist": "The request task does not exist!"
    });
i18n.addResources('de', 'no-usertask-given', {
      "hint": "Leider ist derzeit kein Zugriff auf die Aufgabe möglich!",
      "retry": "Nochmals probieren...",
      "task-does-not-exist": "Die angeforderte Aufgabe existiert nicht!"
    });

const NoUserTaskGiven = ({
  loading = false,
  retry,
}: {
  loading?: boolean,
  retry?: () => void
}) => {
  
  const { t } = useTranslation('no-usertask-given');
  
  const { showLoadingIndicator } = useAppContext();
  
  useEffect(() => {
      showLoadingIndicator(loading);
    }, [ showLoadingIndicator, loading ]);

  return (
      <UserTaskAppLayout>
        <Box
           fill='horizontal'
           margin='medium'
           pad='medium'
           gap='medium'
           align="center">
          {
            loading
                ? undefined
                : retry
                ? <>
                    <Box>
                      { t('hint') }
                    </Box>
                    <Button
                        label={ t('retry') }
                        onClick={ () => retry() } />
                  </>
                : <Text
                      weight='bold'>
                    { t('task-does-not-exist') }
                  </Text>
          }
        </Box>
      </UserTaskAppLayout>);

}

export { NoUserTaskGiven };
