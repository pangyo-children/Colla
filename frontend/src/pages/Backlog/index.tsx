import React, { useEffect, useState } from 'react';

import { useLocation } from 'react-router-dom';
import { getTasksGroupByStory } from '../../apis/task';
import { BacklogFeature } from '../../components/BacklogFeature';
import Header from '../../components/Header';
import Issue from '../../components/Issue';
import { SideBar } from '../../components/SideBar';
import { StateType } from '../../types/project';
import { SimpleTaskType, StoryTaskType } from '../../types/task';
import { Container, Wrapper } from './style';

const Backlog = () => {
    const { state } = useLocation<StateType>();
    const [backlogTaskList, setBacklogTaskList] = useState<Array<StoryTaskType | SimpleTaskType>>([]);

    useEffect(() => {
        (async () => {
            const res = await getTasksGroupByStory(state.projectId);
            setBacklogTaskList(res.data);
        })();
    }, []);

    return (
        <>
            <Header />
            <SideBar />
            <BacklogFeature setBacklogTaskList={setBacklogTaskList} />
            <Container>
                <Wrapper>
                    {backlogTaskList.length > 0 && 'story' in backlogTaskList[0]
                        ? (backlogTaskList as Array<StoryTaskType>).map(({ story, taskList }: StoryTaskType, idx) => (
                              <div key={idx}>
                                  <Issue title={story} story />
                                  {taskList.map(({ id, title, priority, managerAvatar, tags }: SimpleTaskType) => (
                                      <Issue
                                          key={id}
                                          title={title}
                                          priority={priority}
                                          manager={managerAvatar}
                                          tags={tags}
                                      />
                                  ))}
                              </div>
                          ))
                        : (backlogTaskList as Array<SimpleTaskType>).map(
                              ({ id, title, priority, managerAvatar, tags }: SimpleTaskType) => (
                                  <Issue
                                      key={id}
                                      title={title}
                                      priority={priority}
                                      manager={managerAvatar}
                                      tags={tags}
                                  />
                              ),
                          )}
                </Wrapper>
            </Container>
        </>
    );
};

export default Backlog;
